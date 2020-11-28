package io.opencaesar.oml2papyrus;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.FeatureProperty;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.QuotedLiteral;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationCardinalityRestrictionAxiom;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.SpecializableTerm;
import io.opencaesar.oml.Structure;
import io.opencaesar.oml.StructuredProperty;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlIndex;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml2papyrus.util.ProfileUtils;

public class Oml2PapyrusConverter {

	private static final String IS_STEREOTYPE = "http://www.eclipse.org/uml2/5.0.0/UML-Annotations#isStereotype";
	private static final String IS_STEREOTYPE_OF = "http://www.eclipse.org/uml2/5.0.0/UML-Annotations#isStereotypeOf";

	private Ontology rootOntology;
	private File papyrusFolder;
	private ResourceSet papyrusResourceSet;
	private Model umlModel;
	private Logger logger;
	private Map<Entity, Class> converted = new HashMap<>();
	private Map<Vocabulary,Package> voc2Package = new HashMap<>();

	public Oml2PapyrusConverter(Ontology rootOntology, File papyrusFolder, ResourceSet papyrusResourceSet, Logger logger) {
		this.rootOntology = rootOntology;
		this.papyrusFolder = papyrusFolder;
		this.papyrusResourceSet = papyrusResourceSet;
		this.logger = logger;
	}

	public Resource convert() throws Exception {
		// Create the UML resource set
		ProfileUtils.initResourceSet(papyrusResourceSet);
		umlModel = ProfileUtils.getUMLMetamodel(papyrusResourceSet);
		// Create profile
		URI profileUri = URI.createFileURI(papyrusFolder.getAbsolutePath() + File.separator + rootOntology.getPrefix()+ '.' + UMLResource.PROFILE_FILE_EXTENSION);
		Profile profile = ProfileUtils.createProfile(papyrusResourceSet, profileUri, rootOntology.getPrefix(), rootOntology.getIri());
		logger.info("Profile "+profile.getName()+" was created");
		
		// Create the stereotypes
		Collection<StereoTypesInfo> entryPoints = getEntryPoints(rootOntology);
		for (StereoTypesInfo entry : entryPoints) {
			System.out.println(entry);
		}

		for (StereoTypesInfo entryPoint : entryPoints) {
			// create steroetype
			Vocabulary voc = entryPoint.vocabulary;
			Entity entity = entryPoint.entity;
			Package pkg = getPackageForVoc(voc, profile);
			Stereotype stereotype = ProfileUtils.createStereotype(pkg,
																  entity.getName(),
																  entity instanceof Aspect,
																  entryPoint.metaClasses);
			logger.debug("Stereotype "+stereotype.getName()+" was created");
		}
		
		for (StereoTypesInfo entryPoint : entryPoints) {
			Entity entity = entryPoint.entity;
			converEntity(profile, entity);
		}

		// Define the profile after all elements have been created
		profile.define();
		converted.clear();
		voc2Package.clear();
		return profile.eResource();
	}
	
	private Package getPackageForVoc(Vocabulary voc, Profile profile) {
		Package pkg = voc2Package.get(voc);
		if (pkg == null) {
			pkg = getPackage(voc.getIri(), profile);
			voc2Package.put(voc, pkg);
		}
		return pkg;
	}
	
	private Package getPackage(String iri, Package pkg) {
		int i = iri.lastIndexOf("/");
		if (iri.length() == i+1) {
			return pkg;
		}
		if (i > 0) {
			pkg = getPackage(iri.substring(0, i), pkg);
		}
		String name = iri.substring(i+1);
		Package newPkg = (Package) pkg.getPackagedElement(name, false, UMLPackage.Literals.PACKAGE, false);
		if (newPkg == null) {
			newPkg = ProfileUtils.createPackage(pkg, name, iri);
		}
		return newPkg;
	}
	
	private Class converEntity(Profile profile, Entity entity) {
		return converEntity(profile, entity, true);
	}
	
	private Class converEntity(Profile profile, Entity entity, boolean handleSpecilizations) {
		if (converted.containsKey(entity)) {
			return converted.get(entity);
		}
		Package containerPackage = getPackageForVoc(entity.getOwningVocabulary(), profile);
		Class clazz = (Class) containerPackage.getPackagedElement(entity.getName());
		if (clazz == null) {
			clazz = ProfileUtils.createClass(containerPackage, entity.getName(), entity instanceof Aspect);
		}
		converted.put(entity,clazz);
		mapProperties(profile, clazz, entity);
		mapRelationShips(profile, clazz,entity);
		if (handleSpecilizations) {
			convertSpecializations(profile, entity);
		}
		return clazz;
	}
	

	private void mapRelationShips(Profile profile, Class clazz, Entity entity) {
		List<Relation> relations = OmlSearch.findRelationsWithSource(entity);
		for (Relation relation : relations) {
			Entity range = relation.getRange();
			Class clazz1 = converEntity(profile, range);
			RelationEntity relEntity = null; 
			if (relation instanceof ForwardRelation) {
				ForwardRelation fRel = (ForwardRelation)relation;
				relEntity = fRel.getRelationEntity();
				System.out.println(fRel);
			}else if (relation instanceof ReverseRelation) {
				ReverseRelation revRelation = (ReverseRelation)relation;
				relEntity = revRelation.getRelationEntity();
			}
			
			if (relEntity!=null) {
				ForwardRelation fwdRel = relEntity.getForwardRelation();
				ReverseRelation revRel = relEntity.getReverseRelation();
				boolean isFunctional = relEntity.isFunctional();
				String end1Name = fwdRel.getName();
				boolean end1Navigable = true;
				String end2Name = null;
				boolean end2Navigable = false;
				if (revRel!=null) {
					end2Name = revRel.getName();
					end2Navigable = true;
				}
				int end1Lower = 0 ;
				int end1Upper = -1;
				if (isFunctional) {
					end1Upper = 1;
				}
				
				List<RelationCardinalityRestrictionAxiom> card = OmlSearch.findRelationCardinalityRestrictionAxiomsWithRange(relEntity);
				for(RelationCardinalityRestrictionAxiom axiom : card) {
					if(axiom.getKind()==CardinalityRestrictionKind.MIN) {
						end1Lower = (int)axiom.getCardinality();
					}else if (axiom.getKind()==CardinalityRestrictionKind.MAX) {
						end1Upper = (int)axiom.getCardinality();
					}
				}

				int end2Lower = 0 ;
				int end2Upper = 1;
				clazz.createAssociation(end1Navigable, AggregationKind.NONE_LITERAL, end1Name, end1Lower, end1Upper, clazz1,
										end2Navigable, AggregationKind.NONE_LITERAL, end2Name, end2Lower, end2Upper);	
			}
		}
		
	}

	private void mapProperties(Package pkg, Class clazz, Entity entity) {
		 List<FeatureProperty> props = OmlIndex.findFeaturePropertiesWithDomain(entity);
		 for (FeatureProperty prop : props) {
			 if (prop instanceof ScalarProperty) {
				 ScalarProperty sProp = (ScalarProperty)prop;
				 Scalar range = sProp.getRange();
				 PrimitiveType rangeClass = getTypeForRange(pkg,range);
				 clazz.createOwnedAttribute(prop.getName(),rangeClass);
				 System.out.println("Range : " + range.getName());
			 }else if (prop instanceof StructuredProperty) {
				 StructuredProperty stProp = (StructuredProperty)prop;
				 Structure range = stProp.getRange();
				 System.out.println("Range : " + range.getName());
				 
			 } 
			 System.out.println(prop.getName());
		 }
		
	}

	private PrimitiveType getTypeForRange(Package pkg, Scalar range) {
		switch (range.getName()) {
		case "string":
			return (PrimitiveType)umlModel.getMember("String");
		case "integer":
			return (PrimitiveType)umlModel.getMember("Integer");
		case "boolean":
			return (PrimitiveType)umlModel.getMember("Boolean");
		}
		return null;
	}

	private void convertSpecializations(Profile profile, Entity entity) {
		List<SpecializableTerm> specTerms = OmlSearch.findAllSpecializedTerms(entity);
		for (SpecializableTerm term : specTerms) {
			if (term instanceof Entity) {
				Entity superEntity = (Entity)term;
				// get the super entity package
				converEntity(profile, superEntity, false);
				logger.debug(superEntity.getName());
			}
		}
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

	/**
	 * return all annotated Entities as Stereo types
	 * 
	 * @param rootOntology the root Ontology
	 * @return all entry points represented as StereoType Entry
	 */
	private Collection<StereoTypesInfo> getEntryPoints(Ontology rootOntology) {
		List<Vocabulary> directVoc = OmlRead.getAllImportedOntologies(rootOntology).stream()
				.filter(ontology -> ontology instanceof Vocabulary).map(ontology -> (Vocabulary) ontology)
				.collect(Collectors.toList());
		Map<Entity, StereoTypesInfo> entityToInfo = new LinkedHashMap<>();

		// now we have all direct voc let's find the stereo types CI
		for (Vocabulary voc : directVoc) {
			// get all voc entities
			List<Entity> entities = voc.getOwnedStatements().stream().filter(statement -> {
				return statement instanceof Entity;
			}).map(statement -> (Entity) statement).collect(Collectors.toList());
			// not all entities will be sterotyped we need to find the sterotypes ones
			for (Entity entity : entities) {
				// find it is annotations
				OmlSearch.findAnnotations(entity).forEach(annotation -> {
					if (IS_STEREOTYPE.equals(OmlRead.getIri(annotation.getProperty()))) {
						entityToInfo.put(entity, new StereoTypesInfo(voc, entity));
					} else if (IS_STEREOTYPE_OF.equals(OmlRead.getIri(annotation.getProperty()))) {
						Literal value = annotation.getValue();
						if (value instanceof QuotedLiteral) {
							QuotedLiteral qLiteral = (QuotedLiteral) value;
							String sValue = qLiteral.getValue();
							StereoTypesInfo info = entityToInfo.get(entity);
							if (info == null) {
								info = new StereoTypesInfo(voc, entity);
								entityToInfo.put(entity, info);
							}
							info.addMetaClass(sValue.substring(sValue.indexOf(':') + 1));
						}
					}
				});
			}
		}
		return entityToInfo.values();
	}

}
