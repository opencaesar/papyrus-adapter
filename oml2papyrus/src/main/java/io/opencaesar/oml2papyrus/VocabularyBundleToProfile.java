package io.opencaesar.oml2papyrus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.resource.UMLResource;

import io.opencaesar.oml.Annotation;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.EnumeratedScalar;
import io.opencaesar.oml.FeatureProperty;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.QuotedLiteral;
import io.opencaesar.oml.RelationCardinalityRestrictionAxiom;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationRestrictionAxiom;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.SpecializableTerm;
import io.opencaesar.oml.Structure;
import io.opencaesar.oml.StructuredProperty;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.VocabularyBundle;
import io.opencaesar.oml.util.OmlIndex;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml2papyrus.util.ProfileUtils;
import io.opencaesar.oml2papyrus.util.UmlUtils;

public class VocabularyBundleToProfile {

	private static final String IS_STEREOTYPE = "http://www.eclipse.org/uml2/5.0.0/UML-Annotations#isStereotype";
	private static final String IS_STEREOTYPE_OF = "http://www.eclipse.org/uml2/5.0.0/UML-Annotations#isStereotypeOf";

	private VocabularyBundle rootOntology;
	private File outputFolder;
	private ResourceSet outputResourceSet;
	private Logger logger;

	private Model umlMetaModel;
	private Map<Entity, Class> converted = new HashMap<>();
	private Map<Vocabulary, Package> voc2Package = new HashMap<>();

	private static Set<String> vocsToSkip = new HashSet<>();
	static {
		vocsToSkip.add("http://www.eclipse.org/uml2/5.0.0/Types");
		vocsToSkip.add("http://www.eclipse.org/uml2/5.0.0/UML");
		vocsToSkip.add("http://www.eclipse.org/uml2/5.0.0/UML-Annotations");
	}

	public VocabularyBundleToProfile(VocabularyBundle rootOntology, File outputFolder, ResourceSet outputResourceSet,
			Logger logger) {
		this.rootOntology = rootOntology;
		this.outputFolder = outputFolder;
		this.outputResourceSet = outputResourceSet;
		this.logger = logger;
	}

	public Resource convert() throws Exception {
		// Clear all caches
		converted.clear();
		voc2Package.clear();

		// Get the UML metamodel
		umlMetaModel = ProfileUtils.getUMLMetamodel(outputResourceSet);

		// Create parent folder
		URI iri = URI.createURI(rootOntology.getIri());
		String authority = iri.authority();
		List<String> segments = iri.segmentsList();
		File parentFolder = new File(outputFolder.getPath()+File.separator+authority+File.separator+String.join(File.separator, segments.subList(0,  segments.size()-1)));
		parentFolder.mkdirs();

		// Create resource
		URI outputResourceUri = URI.createFileURI(parentFolder.getAbsolutePath()+File.separator+iri.lastSegment()+'.'+UMLResource.PROFILE_FILE_EXTENSION);
		Resource outputResource = outputResourceSet.createResource(outputResourceUri);
		if (outputResource != null) {
			logger.info(outputResourceUri+" was created");
		} else {
			return null;
		}

		// Create the profile
		Profile profile = ProfileUtils.createProfile(outputResourceSet, outputResourceUri, rootOntology.getPrefix(), rootOntology.getIri());

		// Populate the profile
		populateProfile(profile);

		// Define the profile after all elements have been created
		profile.define();

		return profile.eResource();
	}

	private void populateProfile(Profile profile) {

		List<Vocabulary> allVoc = OmlRead.getAllImportedOntologies(rootOntology).stream()
				.filter(ontology -> ontology instanceof Vocabulary).map(ontology -> (Vocabulary) ontology)
				.collect(Collectors.toList());
		// now we have all direct voc let's find the stereo types CI
		for (Vocabulary voc : allVoc) {
			logger.debug("Converting  : " + voc.getIri());
			// create the package for the voc
			if (vocsToSkip.contains(voc.getIri())) {
				logger.debug("Skipping");
				continue;
			}
			Package pkg = null;
			final Set<java.lang.Class> classes = new HashSet();
			// get all voc entities
			List<Entity> entities = voc.getOwnedStatements().stream().filter(statement -> {
				if (statement instanceof EnumeratedScalar) {
					System.out.println(statement.getClass());
				}
				classes.add(statement.getClass());
				return statement instanceof Entity;
			}).map(statement -> (Entity) statement).collect(Collectors.toList());
			// go over all entities
			for (java.lang.Class clazz: classes) {
				System.out.println(clazz.getName());
			}
			if (!entities.isEmpty()) {
				pkg = getPackageForVoc(voc, profile);
			}
			for (Entity entity : entities) {
				logger.debug("Converting : " + entity.getName());
				StereoTypesInfo infoHolder = getStereoTypeInfo(voc, entity);
				if (infoHolder != null) {
					// SteroType
					Stereotype stereotype = ProfileUtils.createStereotype(pkg, entity.getName(),
							entity instanceof Aspect, infoHolder.metaClasses);
					logger.debug("Stereotype " + stereotype.getName() + " was created");
				}
				converEntity(profile, entity);
			}
			logger.debug("================================================");
		}

		// Relationships
		updateRelationships(allVoc);

		// update the generalizations
		updateAllGeneralizations();
	}

	private void updateRelationships(List<Vocabulary> allVoc) {
		for (Vocabulary voc : allVoc) {
			logger.info("Converting  : " + voc.getIri());
			// create the package for the voc
			if (vocsToSkip.contains(voc.getIri())) {
				logger.info("Skipping");
				continue;
			}
			// get all voc entities
			List<RelationEntity> entities = voc.getOwnedStatements().stream().filter(statement -> {
				return statement instanceof RelationEntity;
			}).map(statement -> (RelationEntity) statement).collect(Collectors.toList());
			for (RelationEntity entity : entities) {
				logger.info("Converting Relation: " + entity.getName());
				logger.info(entity);
				ForwardRelation srcRel = entity.getForwardRelation();
				ReverseRelation trgRel = entity.getReverseRelation();
				Entity src = srcRel.getDomain();
				Entity trgt = srcRel.getRange();
				Class srcClass = converted.get(src);
				Class trgClass = converted.get(trgt);
				boolean isFunctional = entity.isFunctional();
				String end1Name = srcRel.getName();
				boolean end1Navigable = true;
				String end2Name = "";
				boolean end2Navigable = false;
				int end1Lower = 0;
				int end1Upper = -1;
				int end2Lower = 0;
				int end2Upper = -1;
				if (isFunctional) {
					end2Upper = 1;
				}
				List<RelationRestrictionAxiom> srcCard = OmlSearch.findRelationRestrictionAxiomsWithRelation(srcRel);
				for (RelationRestrictionAxiom axiom : srcCard) {
					if (axiom instanceof RelationCardinalityRestrictionAxiom) {
						RelationCardinalityRestrictionAxiom cardAxiom = (RelationCardinalityRestrictionAxiom) axiom;
						if (cardAxiom.getKind() == CardinalityRestrictionKind.MIN) {
							end1Lower = (int) cardAxiom.getCardinality();
						} else if (cardAxiom.getKind() == CardinalityRestrictionKind.MAX) {
							end1Upper = (int) cardAxiom.getCardinality();
						}
					}
				}
				if (trgRel != null) {
					end2Name = trgRel.getName();
					end2Navigable = true;
					List<RelationRestrictionAxiom> trgtCard = OmlSearch
							.findRelationRestrictionAxiomsWithRelation(trgRel);
					for (RelationRestrictionAxiom axiom : trgtCard) {
						if (axiom instanceof RelationCardinalityRestrictionAxiom) {
							RelationCardinalityRestrictionAxiom cardAxiom = (RelationCardinalityRestrictionAxiom) axiom;
							if (cardAxiom.getKind() == CardinalityRestrictionKind.MIN) {
								end2Lower = (int) cardAxiom.getCardinality();
							} else if (cardAxiom.getKind() == CardinalityRestrictionKind.MAX) {
								end2Upper = (int) cardAxiom.getCardinality();
							}
						}
					}
				}
				srcClass.createAssociation(end1Navigable, AggregationKind.NONE_LITERAL, end1Name, end1Lower, end1Upper,
						trgClass, end2Navigable, AggregationKind.NONE_LITERAL, end2Name, end2Lower, end2Upper);
			}
		}
	}

	private StereoTypesInfo getStereoTypeInfo(Vocabulary voc, Entity entity) {
		StereoTypesInfo infoHolder = null;
		List<Annotation> annotations = OmlSearch.findAnnotations(entity);
		for (Annotation annotation : annotations) {
			if (IS_STEREOTYPE.equals(OmlRead.getIri(annotation.getProperty()))) {
				infoHolder = new StereoTypesInfo(voc, entity);
			} else if (IS_STEREOTYPE_OF.equals(OmlRead.getIri(annotation.getProperty()))) {
				Literal value = annotation.getValue();
				if (value instanceof QuotedLiteral) {
					QuotedLiteral qLiteral = (QuotedLiteral) value;
					String sValue = qLiteral.getValue();
					if (infoHolder == null) {
						infoHolder = new StereoTypesInfo(voc, entity);
					}
					infoHolder.addMetaClass(sValue.substring(sValue.indexOf(':') + 1));
				}
			}
		}
		return infoHolder;
	}

	private void updateAllGeneralizations() {
		Set<Entity> keys = converted.keySet();
		for (Entity entity : keys) {
			Class clazz = converted.get(entity);
			List<SpecializableTerm> specTerms = OmlSearch.findSpecializedTerms(entity);
			for (SpecializableTerm term : specTerms) {
				if (term instanceof Entity) {
					Entity superEntity = (Entity) term;
					Class superClazz = converted.get(superEntity);
					clazz.createGeneralization(superClazz);
				}
			}
		}
	}

	private Package getPackageForVoc(Vocabulary voc, Profile profile) {
		Package pkg = voc2Package.get(voc);
		if (pkg == null) {
			pkg = UmlUtils.getPackage(voc.getIri(), profile);
			voc2Package.put(voc, pkg);
		}
		return pkg;
	}

	private Class converEntity(Profile profile, Entity entity) {
		if (converted.containsKey(entity)) {
			return converted.get(entity);
		}
		Package containerPackage = getPackageForVoc(entity.getOwningVocabulary(), profile);
		Class clazz = (Class) containerPackage.getPackagedElement(entity.getName());
		if (clazz == null) {
			clazz = ProfileUtils.createClass(containerPackage, entity.getName(), entity instanceof Aspect);
		}
		converted.put(entity, clazz);
		mapProperties(profile, clazz, entity);
		return clazz;
	}

	private void mapProperties(Package pkg, Class clazz, Entity entity) {
		List<FeatureProperty> props = OmlIndex.findFeaturePropertiesWithDomain(entity);
		for (FeatureProperty prop : props) {
			if (prop instanceof ScalarProperty) {
				ScalarProperty sProp = (ScalarProperty) prop;
				Scalar range = sProp.getRange();
				PrimitiveType rangeClass = getTypeForRange(pkg, range);
				clazz.createOwnedAttribute(prop.getName(), rangeClass);
			} else if (prop instanceof StructuredProperty) {
				StructuredProperty stProp = (StructuredProperty) prop;
				Structure range = stProp.getRange();
			}
		}

	}

	private PrimitiveType getTypeForRange(Package pkg, Scalar range) {
		switch (range.getName()) {
		case "string":
			return (PrimitiveType) umlMetaModel.getMember("String");
		case "integer":
			return (PrimitiveType) umlMetaModel.getMember("Integer");
		case "boolean":
			return (PrimitiveType) umlMetaModel.getMember("Boolean");
		}
		return null;
	}

	private static class StereoTypesInfo {
		public Vocabulary vocabulary;
		public Entity entity;
		public Set<String> metaClasses = new HashSet<>();

		public StereoTypesInfo(Vocabulary voc, Entity entity) {
			this.vocabulary = voc;
			this.entity = entity;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(vocabulary.getIri() + " -> " + entity.getName());
			if (metaClasses.size() > 0) {
				builder.append("\n StereoTypeOf : ");
				metaClasses.forEach(a -> builder.append(a));
			}
			return builder.toString();
		}

		public void addMetaClass(String metaClassName) {
			metaClasses.add(metaClassName);
		}
	}
}
