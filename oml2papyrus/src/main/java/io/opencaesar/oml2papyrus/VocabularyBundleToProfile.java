package io.opencaesar.oml2papyrus;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.resource.UMLResource;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.EnumeratedScalar;
import io.opencaesar.oml.FacetedScalar;
import io.opencaesar.oml.FeatureProperty;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.QuotedLiteral;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.RelationCardinalityRestrictionAxiom;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationRangeRestrictionAxiom;
import io.opencaesar.oml.RelationRestrictionAxiom;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.ScalarPropertyCardinalityRestrictionAxiom;
import io.opencaesar.oml.ScalarPropertyRestrictionAxiom;
import io.opencaesar.oml.SpecializableTerm;
import io.opencaesar.oml.StructuredProperty;
import io.opencaesar.oml.Type;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.VocabularyBundle;
import io.opencaesar.oml.util.OmlIndex;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml2papyrus.util.ProfileUtils;
import io.opencaesar.oml2papyrus.util.UmlUtils;

public class VocabularyBundleToProfile {

	private static final String IS_STEREOTYPE_OF = "http://www.eclipse.org/uml2/5.0.0/UML-Annotations#isStereotypeOf";

	private VocabularyBundle rootOntology;
	private File outputFolder;
	private ResourceSet outputResourceSet;
	private Logger logger;

	private Model umlMetaModel;
	private Map<io.opencaesar.oml.Type, Classifier> converted = new HashMap<>();
	private Map<RelationEntity, AssociationInfo> relationToAssociationInfo = new HashMap<>();
	//private Map<Vocabulary, Package> voc2Package = new HashMap<>();

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
	
	
	private boolean isFiltered(Vocabulary voc) {
		if (vocsToSkip.contains(voc.getIri())) {
			logger.debug("Skipping");
			return true;
		}
		return false;
	}

	private void populateProfile(Profile profile) {

		List<Vocabulary> allVoc = OmlRead.getAllImportedOntologies(rootOntology).stream()
				.filter(ontology -> ontology instanceof Vocabulary)
				.map(ontology -> (Vocabulary) ontology)
				.collect(Collectors.toList());
		// now we have all direct voc let's find the stereotypes CI
		for (Vocabulary voc : allVoc) {
			logger.debug("Converting  : " + voc.getIri());
			// create the package for the voc
			if (isFiltered(voc)) {
				continue;
			}
			
			// get all voc types
			List<Type> types = voc.getOwnedStatements().stream()
					.filter(s -> s instanceof Type)
					.map(s -> (Type)s)
					.filter(t -> canConvert(t))
					.collect(Collectors.toList());
			
			if (!types.isEmpty()) {
				Package pkg = UmlUtils.getPackage(voc.getIri(), profile);

				for (Type type : types) {
					convertType(profile, voc, pkg, type);
				};
			}
			
			logger.debug("================================================");
		}

		// Properties
		mapProperties(allVoc);
		
		// Relationships
		updateRelationships(allVoc);

		// update the generalizations
		updateAllGeneralizations();
		
		// now we need to deal with restrictions (Range and Value)
		updateRestrictions(allVoc);
	}

	private void updateRestrictions(List<Vocabulary> allVoc) {
		Set<Type> convertedEntities = converted.keySet();
		for (Type type : convertedEntities) {
			if (type instanceof Entity) {
				Entity entity = (Entity)type;
				List<RelationRestrictionAxiom> restrictions = entity.getOwnedRelationRestrictions();
				for (RelationRestrictionAxiom axiom : restrictions) {
					if (axiom instanceof RelationRangeRestrictionAxiom) {
						// only for kind.all
						// for sum this will be a constraint in the future
						RelationRangeRestrictionAxiom rangeRest = (RelationRangeRestrictionAxiom)axiom;
						if (rangeRest.getKind()==RangeRestrictionKind.ALL) {
							RelationEntity relEntity = OmlRead.getRelationEntity( rangeRest.getRelation());
							Entity newRange = rangeRest.getRange();
							AssociationInfo info = relationToAssociationInfo.get(relEntity);	
							Classifier src = converted.get(entity);
							Classifier newTrgt = converted.get(newRange);
							src.createAssociation(info.end1Navigable,
												  AggregationKind.NONE_LITERAL,
												  info.end1Name,
												  info.end1Lower,
												  info.end1Upper,
												  newTrgt,
												  info.end2Navigable,
												  AggregationKind.NONE_LITERAL,
												  info.end2Name,
												  info.end2Lower,
												  info.end2Upper);
						}
					}
				}
			}
		}
		// get all voc entities
	}

	private boolean canConvert(Type type) {
		return type instanceof Entity ||
				type instanceof EnumeratedScalar;
	}

	private void convertType(Profile profile, Vocabulary voc, Package pkg, Type type) {
		if (type instanceof Entity) {
			convertEntity(profile, voc, pkg, (Entity)type);
		} else if (type instanceof EnumeratedScalar) {
			convertEnum(profile, voc, pkg, (EnumeratedScalar)type);
		}
	}
	
	private void convertEnum(Profile profile, Vocabulary voc, Package pkg, EnumeratedScalar enumType) {
		if (converted.containsKey(enumType)) {
			return;
		}

		String name = enumType.getName();
		EList<Literal> literals = enumType.getLiterals();
		logger.debug("Enum : " + name);
		final Enumeration umlEnum = pkg.createOwnedEnumeration(name);
		literals.forEach(literal -> {
			umlEnum.createOwnedLiteral(OmlRead.getLexicalValue(literal));
		});

		converted.put(enumType, umlEnum);
	}

	private void convertEntity(Profile profile, Vocabulary voc, Package pkg, Entity entity) {
		if (converted.containsKey(entity)) {
			return;
		}

		logger.debug("Converting : " + entity.getName());
		StereoTypesInfo infoHolder = getStereoTypeInfo(voc, entity);

		Stereotype stereotype = ProfileUtils.createStereotype(pkg, entity.getName(), entity instanceof Aspect, infoHolder.metaClasses);
		logger.debug("Stereotype " + stereotype.getName() + " was created");

		converted.put(entity, stereotype);
	}

	private void updateRelationships(List<Vocabulary> allVoc) {
		// value restriction is a place holder
		for (Vocabulary voc : allVoc) {
			logger.debug("Converting  : " + voc.getIri());
			// create the package for the voc
			if (isFiltered(voc)) {
				continue;
			}
			// get all voc entities
			List<RelationEntity> entities = voc.getOwnedStatements().stream().filter(statement -> {
				return statement instanceof RelationEntity;
			}).map(statement -> (RelationEntity) statement).collect(Collectors.toList());
			for (RelationEntity entity : entities) {
				logger.debug("Converting Relation: " + entity.getName());
				logger.debug(entity);
				ForwardRelation srcRel = entity.getForwardRelation();
				ReverseRelation trgRel = entity.getReverseRelation();
				Entity src = srcRel.getDomain();
				Entity trgt = srcRel.getRange();
				Class srcClass = (Class) converted.get(src);
				Class trgClass = (Class) converted.get(trgt);
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
						Entity range = cardAxiom.getRelation().getDomain();
						if (range==src) {
							if (cardAxiom.getKind() == CardinalityRestrictionKind.MIN) {
								end1Lower = (int) cardAxiom.getCardinality();
							} else if (cardAxiom.getKind() == CardinalityRestrictionKind.MAX) {
								end1Upper = (int) cardAxiom.getCardinality();
							}
						}
					}
				}
				if (trgRel != null) {
					// match with the range
					end2Name = trgRel.getName();
					end2Navigable = true;
					List<RelationRestrictionAxiom> trgtCard = OmlSearch.findRelationRestrictionAxiomsWithRelation(trgRel);
					for (RelationRestrictionAxiom axiom : trgtCard) {
						if (axiom instanceof RelationCardinalityRestrictionAxiom) {
							RelationCardinalityRestrictionAxiom cardAxiom = (RelationCardinalityRestrictionAxiom) axiom;
							Entity domain = cardAxiom.getRelation().getDomain();
							if (domain==trgt) {
								if (cardAxiom.getKind() == CardinalityRestrictionKind.MIN) {
									end2Lower = (int) cardAxiom.getCardinality();
								} else if (cardAxiom.getKind() == CardinalityRestrictionKind.MAX) {
									end2Upper = (int) cardAxiom.getCardinality();
								}
							}
						}
					}
				}
				AssociationInfo aInfo = new AssociationInfo(trgClass,end1Name,end1Navigable,end1Lower,end1Upper,end2Name,end2Navigable,end2Lower,end2Upper);
				relationToAssociationInfo.put(entity, aInfo);
				srcClass.createAssociation(end1Navigable, 
										   AggregationKind.NONE_LITERAL,
										   end1Name,
										   end1Lower,
										   end1Upper,
										   trgClass,
										   end2Navigable,
										   AggregationKind.NONE_LITERAL,
										   end2Name,
										   end2Lower,
										   end2Upper);
			}
		}
	}
	
	
	private static class AssociationInfo {
		public Class trgClass;
		public String end1Name = "";
		public boolean end1Navigable = true;
		public String end2Name = "";
		public boolean end2Navigable = false;
		public int end1Lower = 0;
		public int end1Upper = -1;
		public int end2Lower = 0;
		public int end2Upper = -1;
		public AssociationInfo(Class trgtClass, String end1Name, boolean end1Navigable, int end1Lower, int end1Upper,
							   String end2Name, boolean end2NAvigable, int end2Lower, int end2Upper) {
			this.trgClass = trgtClass;
			this.end1Name = end1Name;
			this.end1Navigable = end1Navigable;
			this.end1Lower = end1Lower;
			this.end1Upper = end1Upper;
			this.end2Name = end2Name;
			this.end2Navigable = end2NAvigable;
			this.end2Lower = end2Lower;
			this.end2Upper = end2Upper;
		}
		
		@Override
		public String toString() {
			return "Relation Entity for Target : " + trgClass.getName();
		}
	}

	private StereoTypesInfo getStereoTypeInfo(Vocabulary voc, Entity entity) {
		StereoTypesInfo infoHolder = new StereoTypesInfo(voc, entity);
		List<Literal> values = OmlSearch.findAnnotationValuesForIri(entity, IS_STEREOTYPE_OF);
		for (Literal value : values) {
			if (value instanceof QuotedLiteral) {
				QuotedLiteral qLiteral = (QuotedLiteral) value;
				String sValue = qLiteral.getValue();
				infoHolder.addMetaClass(sValue.substring(sValue.indexOf(':') + 1));
			}
		}
		return infoHolder;
	}

	private void updateAllGeneralizations() {
		Set<Type> types = converted.keySet();
		for (Type type : types) {
			Classifier subClassifier = converted.get(type);
			List<SpecializableTerm> generalTerms = OmlSearch.findGeneralTerms(type);
			for (SpecializableTerm generalTerm : generalTerms) {
				if (generalTerm instanceof Type) {
					Classifier generalClassifier = converted.get(generalTerm);
					subClassifier.createGeneralization(generalClassifier);
				}
			}
		}
	}

	private void mapProperties(List<Vocabulary> allVoc) {
		//TODO: handle Key properties
		for (Vocabulary voc : allVoc) {
			// create the package for the voc
			if (isFiltered(voc)) {
				continue;
			}
			// get all feature properties
			List<FeatureProperty> props = voc.getOwnedStatements().stream()
					.filter(s -> s instanceof FeatureProperty)
					.map(s -> (FeatureProperty)s)
					.collect(Collectors.toList());
			for (FeatureProperty prop : props) {
				if (prop instanceof ScalarProperty) {
					ScalarProperty sProp = (ScalarProperty) prop;
					int upper = -1;
					int lower = 0;
					if (sProp.isFunctional()) {
						upper = 1;
					}else {
						System.out.println(sProp.getName());
					}
					
					List<ScalarPropertyRestrictionAxiom> axioms = OmlIndex.findScalarPropertyRestrictionAxiomsWithProperty(sProp);
					for (ScalarPropertyRestrictionAxiom rest : axioms) {
						if (rest instanceof ScalarPropertyCardinalityRestrictionAxiom ) {
							ScalarPropertyCardinalityRestrictionAxiom card = (ScalarPropertyCardinalityRestrictionAxiom)rest;
							if (sProp.getDomain() == card.getOwningClassifier()) {
								if (card.getKind() == CardinalityRestrictionKind.MIN) {
									lower = (int) card.getCardinality();
								} else if (card.getKind() == CardinalityRestrictionKind.MAX) {
									upper = (int) card.getCardinality();
								}
							}
						}
					}
					
					Classifier classifier = converted.get(sProp.getDomain());
					Scalar range = sProp.getRange();
					DataType rangeClass = getTypeForRange(range);
					if (classifier instanceof Class) {
						Property umlProperty = ((Class)classifier).createOwnedAttribute(prop.getName(), rangeClass);
						umlProperty.setLower(lower);
						umlProperty.setUpper(upper);
						//System.out.println(prop.getName() + " => " + lower + ":" + upper);
					}
				} else if (prop instanceof StructuredProperty) {
					StructuredProperty stProp = (StructuredProperty) prop;
					logger.debug("StructuredProperty:" + stProp.getRange());
				}
			}
		}
	}

	private DataType getTypeForRange(Scalar range) {
		if (range instanceof FacetedScalar) {
			switch (range.getName()) {
			case "string":
				return (PrimitiveType) umlMetaModel.getMember("String");
			case "integer":
				return (PrimitiveType) umlMetaModel.getMember("Integer");
			case "double":
				return (PrimitiveType) umlMetaModel.getMember("Real");
			case "boolean":
				return (PrimitiveType) umlMetaModel.getMember("Boolean");
			}
		} else if (range instanceof EnumeratedScalar) {
			return (DataType) converted.get(range);
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
