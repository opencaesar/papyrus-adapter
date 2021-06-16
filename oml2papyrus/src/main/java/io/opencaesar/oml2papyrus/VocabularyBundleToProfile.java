/**
 * 
 * Copyright 2021 Modelware Solutions and CAE-LIST.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package io.opencaesar.oml2papyrus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.resource.UMLResource;

import io.opencaesar.oml.AnnotatedElement;
import io.opencaesar.oml.Annotation;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.EnumeratedScalar;
import io.opencaesar.oml.FacetedScalar;
import io.opencaesar.oml.FeatureProperty;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.PropertyRestrictionAxiom;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationCardinalityRestrictionAxiom;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationRangeRestrictionAxiom;
import io.opencaesar.oml.RelationRestrictionAxiom;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.ScalarPropertyCardinalityRestrictionAxiom;
import io.opencaesar.oml.ScalarPropertyRangeRestrictionAxiom;
import io.opencaesar.oml.ScalarPropertyRestrictionAxiom;
import io.opencaesar.oml.ScalarPropertyValueRestrictionAxiom;
import io.opencaesar.oml.SpecializableTerm;
import io.opencaesar.oml.Structure;
import io.opencaesar.oml.StructuredProperty;
import io.opencaesar.oml.Type;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.VocabularyBundle;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml2papyrus.util.ProfileUtils;
import io.opencaesar.oml2papyrus.util.UmlUtils;

public class VocabularyBundleToProfile {

	private static final String CONCEPT_POSTFIX = "_Concept";
	private static final String RELATION_POSTFIX = "_Relation";

	private VocabularyBundle rootOntology;
	private File outputFolder;
	private ResourceSet outputResourceSet;
	private Logger logger;

	private Model umlMetaModel;
	private Map<io.opencaesar.oml.Type, Classifier> converted = new HashMap<>();
	private Map<RelationEntity, AssociationInfo> relationToAssociationInfo = new HashMap<>();

	private static Set<String> vocsToSkip = new HashSet<>();
	static {
		vocsToSkip.add(UmlUtils.TYPES_IRI);
		vocsToSkip.add(UmlUtils.UML_IRI);
		vocsToSkip.add(UmlUtils.UML_ANNOTATIONS_IRI);
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
		
		convertAnnotations(profile, rootOntology);
		
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

		List<Vocabulary> allVoc = OmlRead.getAllImportedOntologies(rootOntology, false).stream()
				.filter(ontology ->  {
					return ontology instanceof Vocabulary && !isFiltered((Vocabulary)ontology);
				} )
				.map(ontology -> (Vocabulary) ontology)
				.collect(Collectors.toList());
		
		// now we have all direct voc let's find the stereotypes CI
		for (Vocabulary voc : allVoc) {
			logger.debug("Converting  : " + voc.getIri());
			// create the package for the voc
			if (isFiltered(voc)) {
				continue;
			}
			convertVocabulary(profile, voc);
			logger.debug("================================================");
		}

		// Properties
		convertAllProperties(profile, allVoc);
		
		// Relationships
		updateRelationships(allVoc);

		// update the generalizations
		updateAllGeneralizations();
		
		// deal with relation restrictions (Range and Value)
		updateRestrictions(allVoc);
		
		// add property redefinitions
		addPropertyRedifinitions(profile);
	}

	private void convertAllProperties(Profile profile, List<Vocabulary> allVoc) {
		for (Vocabulary voc : allVoc) {
			List<Type> types = voc.getOwnedStatements().stream()
					.filter(s -> s instanceof Type)
					.map(s -> (Type)s)
					.filter(t -> canConvert(t))
					.collect(Collectors.toList());
			if (!types.isEmpty()) {
				for (Type type : types) {
					if (type instanceof Entity) {
						Package pkg = UmlUtils.getPackage(voc.getIri(), profile);
						convertProperties(pkg,(Entity) type);
					}
				};
			}
		}
		
	}

	private void convertAnnotations(Element umlElement, AnnotatedElement omlElement) {
		List<Annotation> annotations = OmlSearch.findAnnotations(omlElement);
		for (Annotation annotation : annotations) {
			if (annotation.getProperty().getIri().equals(OmlConstants.DC_NS+"description") ||
				annotation.getProperty().getIri().equals(OmlConstants.RDFS_NS+"comment")) {
				Comment comment = umlElement.createOwnedComment();
				comment.setBody(OmlRead.getStringValue(annotation.getValue()));
			}
		}
	}
	
	private void convertVocabulary(Profile profile, Vocabulary voc) {
		// get all voc types
		List<Type> types = voc.getOwnedStatements().stream()
				.filter(s -> s instanceof Type)
				.map(s -> (Type)s)
				.filter(t -> canConvert(t))
				.collect(Collectors.toList());
		if (!types.isEmpty()) {
			Package pkg = UmlUtils.getPackage(voc.getIri(), profile);
			convertAnnotations(pkg, voc);
			for (Type type : types) {
				convertType(profile, voc, pkg, type);
			}
		}
	}
	
	private void convertType(Profile profile, Vocabulary voc, Package pkg, Type type) {
		if (type instanceof Entity) {
			convertEntity(profile, voc, pkg, (Entity)type);
		} else if (type instanceof EnumeratedScalar) {
			convertEnum(pkg,  (EnumeratedScalar)type);
		} else if (type instanceof Structure) {
			convertStructure(profile, voc, pkg, (Structure)type);
		}
	}
	
	private void convertStructure(Profile profile, Vocabulary voc, Package pkg, Structure type) {
		Classifier convertedType = converted.get(type);
		if (convertedType==null) {
			logger.debug("Converting : " + type.getName());
			convertedType = pkg.createOwnedClass(type.getName(), false);
			converted.put(type, convertedType);
			convertAnnotations(convertedType, type);
		}
	}

	private void convertEntity(Profile profile, Vocabulary voc, Package pkg, Entity entity) {
		if (converted.containsKey(entity)) {
			return;
		}
		
		logger.debug("Converting : " + entity.getName());
		StereoTypesInfo infoHolder = getStereoTypeInfo(voc, entity);

		Stereotype stereotype = ProfileUtils.createStereotype(pkg, entity.getName(), entity instanceof Aspect, infoHolder.metaClasses);
		UmlUtils.addNameAnnotationIfNeeded(stereotype);
		converted.put(entity, stereotype);
		logger.debug("Stereotype " + stereotype.getName() + " was created");
		
		convertAnnotations(stereotype, entity);
	}

	private void convertProperties(Package pkg, Entity entity) {
		EList<PropertyRestrictionAxiom> propRestrictions = entity.getOwnedPropertyRestrictions();
		Map<ScalarProperty, List<PropertyRestrictionAxiom>> propToRestirctions = getMappedRestrictions(propRestrictions);
		
		// convert the properties
		List<FeatureProperty> props = OmlSearch.findFeaturePropertiesWithDomain(entity);
		for (FeatureProperty prop : props){
			if (prop instanceof ScalarProperty) {
				convertProperty(pkg, (ScalarProperty)prop, entity, propToRestirctions);
				propToRestirctions.remove(prop);
			}
		}
		
		Set<Entry<ScalarProperty, List<PropertyRestrictionAxiom>>> entries = propToRestirctions.entrySet();
		for (Entry<ScalarProperty, List<PropertyRestrictionAxiom>> entry : entries) {
			convertProperty(pkg, entry.getKey(), entity, propToRestirctions);
		}
	}

	private void convertEnum( Package pkg, EnumeratedScalar enumType) {
		if (converted.containsKey(enumType)) {
			return;
		}
		EList<Literal> literals = enumType.getLiterals();
		final Enumeration umlEnum = pkg.createOwnedEnumeration(enumType.getName());
		UmlUtils.addNameAnnotationIfNeeded(umlEnum);
		logger.debug("Enum : " + umlEnum.getName());

		convertAnnotations(umlEnum, enumType);

		literals.forEach(literal -> {
			EnumerationLiteral umlLiteral = umlEnum.createOwnedLiteral(OmlRead.getStringValue(literal));
			UmlUtils.addNameAnnotationIfNeeded(umlLiteral);
		});

		converted.put(enumType, umlEnum);
	}

	private void convertProperty(Package pkg, ScalarProperty prop, io.opencaesar.oml.Classifier entity, Map<ScalarProperty, List<PropertyRestrictionAxiom>> propToRestirctions) {
		int upper = -1;
		int lower = 0;
		Scalar range = prop.getRange();
		String defaultValue = null;
		
		if (prop.isFunctional()) {
			upper = 1;
		}
		
		List<PropertyRestrictionAxiom> axioms = propToRestirctions.get(prop);
		if (axioms!=null) {
			for (PropertyRestrictionAxiom rest : axioms) {
				if (rest instanceof ScalarPropertyCardinalityRestrictionAxiom ) {
					ScalarPropertyCardinalityRestrictionAxiom card = (ScalarPropertyCardinalityRestrictionAxiom)rest;
					if (card.getKind() == CardinalityRestrictionKind.MIN) {
						lower = (int) card.getCardinality();
					} else if (card.getKind() == CardinalityRestrictionKind.MAX) {
						upper = (int) card.getCardinality();
					}
				} else if (rest instanceof ScalarPropertyRangeRestrictionAxiom) {
					ScalarPropertyRangeRestrictionAxiom rangeAxiom = (ScalarPropertyRangeRestrictionAxiom)rest;
					range = rangeAxiom.getRange();
				} else if (rest instanceof ScalarPropertyValueRestrictionAxiom) {
					ScalarPropertyValueRestrictionAxiom valueAxiom = (ScalarPropertyValueRestrictionAxiom)rest;
					defaultValue = OmlRead.getStringValue(valueAxiom.getValue());
				}
			}
		}
		
		Classifier classifier = converted.get(entity);
		DataType rangeClass = getTypeForRange(range);
		if (classifier instanceof Class) {
			Property umlProperty = ((Class)classifier).createOwnedAttribute(prop.getName(), rangeClass);
			UmlUtils.addNameAnnotationIfNeeded(umlProperty);
			String iri = prop.getIri();
			UmlUtils.addIRIAnnotation(umlProperty, iri);
			convertAnnotations(umlProperty, prop);
			umlProperty.setLower(lower);
			umlProperty.setUpper(upper);
			umlProperty.setDefault(defaultValue);
		}
	}

	

	private void updateRelationships(List<Vocabulary> allVoc) {
		// value restriction is a place holder
		for (Vocabulary voc : allVoc) {
			logger.debug("Converting  : " + voc.getIri());
			// get all voc entities
			List<RelationEntity> entities = voc.getOwnedStatements().stream()
				.filter(statement -> statement instanceof RelationEntity)
				.map(statement -> (RelationEntity) statement)
				.collect(Collectors.toList());
			for (RelationEntity entity : entities) {
				logger.debug("Converting Relation: " + entity.getName());
				logger.debug(entity);
				ReverseRelation srcRel = entity.getReverseRelation();
				ForwardRelation trgRel = entity.getForwardRelation();
				String srcRelationIri = srcRel==null ? "" : srcRel.getIri();
				String targetRelationIri = trgRel==null ? "" : trgRel.getIri();
				Entity src = entity.getSource();
				Entity trgt = entity.getTarget();
				Class srcClass = (Class) converted.get(src);
				Class trgClass = (Class) converted.get(trgt);
				boolean isFunctional = entity.isFunctional();
				String end1Name = "";
				boolean end1Navigable = false;
				String end2Name = "";
				boolean end2Navigable = false;
				int end1Lower = 0;
				int end1Upper = -1;
				int end2Lower = 0;
				int end2Upper = -1;
				if (isFunctional) {
					end2Upper = 1;
				}
				if (srcRel != null) {
					// match with the range
					end1Name = srcRel.getName();
					end1Navigable = true;
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
				AssociationInfo aInfo = new AssociationInfo(srcClass,end1Name,end1Navigable,end1Lower,end1Upper,end2Name,end2Navigable,end2Lower,end2Upper,srcRelationIri,targetRelationIri);
				relationToAssociationInfo.put(entity, aInfo);
				createAssociation(srcClass, trgClass, end1Name, end1Navigable, end2Name, end2Navigable, end1Lower,
						end1Upper, end2Lower, end2Upper,srcRelationIri, targetRelationIri);
			}
		}
	}

	private void updateAllGeneralizations() {
		Set<Type> types = converted.keySet();
		for (Type type : types) {
			if (type instanceof io.opencaesar.oml.Classifier) {
				Classifier subClassifier = converted.get(type);
				List<SpecializableTerm> generalTerms = OmlSearch.findSuperTerms(type);
				for (SpecializableTerm generalTerm : generalTerms) {
					if (generalTerm instanceof Type && !generalTerm.getOwningVocabulary().getIri().equals(UmlUtils.UML_IRI)) {
						Classifier generalClassifier = converted.get(generalTerm);
						
						Generalization gen = subClassifier.createGeneralization(generalClassifier);
						
						convertAnnotations(gen, generalTerm);
					}
				}
			}
		}
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
							boolean reverse = false;
							Relation relation = rangeRest.getRelation();
							if (relation instanceof ReverseRelation) {
								reverse = true;
							}
							RelationEntity relEntity = rangeRest.getRelation().getRelationEntity();
							Entity newRange = rangeRest.getRange();
							AssociationInfo info = relationToAssociationInfo.get(relEntity);	
							Classifier src = converted.get(entity);
							Classifier newTrgt = converted.get(newRange);
							if (reverse) {
								createAssociation(newTrgt, src, info.end1Name, info.end1Navigable, info.end2Name, false, info.end1Lower, info.end1Upper, info.end2Lower, info.end2Upper, info.srcIri, info.trgtIri);
							}else {
								createAssociation(src, newTrgt, info.end1Name, false, info.end2Name, info.end2Navigable, info.end1Lower, info.end1Upper, info.end2Lower, info.end2Upper, info.srcIri, info.trgtIri);
							}
						}
					}
				}
			}
		}
	}

	private static void addPropertyRedifinitions(Profile profile) {
		TreeIterator<EObject> i = profile.eAllContents();
		while (i.hasNext()) {
			EObject o = i.next();
			if (o instanceof Property) {
				Property p = (Property) o;
				Element e = (Element) p.getOwner();
				if (e instanceof Class) {
					Class c = (Class) e;
					List<Property> redefinedProperties = new ArrayList<>();
					for (Class s : c.getSuperClasses()) {
						redefinedProperties.addAll(getPropertiesToRedefine(p, s));
					}
					p.getRedefinedProperties().addAll(redefinedProperties);
				}
			}
		}
	}

	private static List<Property> getPropertiesToRedefine(Property p, Class superClass) {
		List<Property> redefinedProperties = new ArrayList<>();
		Property redefinedProperty = superClass.getOwnedAttributes().stream()
			.filter(p2 -> p2.getName().equals(p.getName()))
			.findAny().orElse(null);
		if (redefinedProperty != null) {
			redefinedProperties.add(redefinedProperty);
		} else {
			for (Class s : superClass.getSuperClasses()) {
				redefinedProperties.addAll(getPropertiesToRedefine(p, s));
			}
		}
		return redefinedProperties;
	}
		
	private boolean canConvert(Type type) {
		return type instanceof Entity || type instanceof EnumeratedScalar || type instanceof Structure;
	}

	private Map<ScalarProperty, List<PropertyRestrictionAxiom>> getMappedRestrictions(
			EList<PropertyRestrictionAxiom> propRestrictions) {
		Map<ScalarProperty,List<PropertyRestrictionAxiom>> propToRestirctions = new HashMap<>();
		for (PropertyRestrictionAxiom propRest : propRestrictions) {
			if (propRest instanceof ScalarPropertyRestrictionAxiom) {
				ScalarPropertyRestrictionAxiom card = (ScalarPropertyRestrictionAxiom) propRest;
				ScalarProperty prop = card.getProperty();
				List<PropertyRestrictionAxiom> restrictions = propToRestirctions.get(prop);
				if (restrictions==null) {
					restrictions = new ArrayList<>();
					propToRestirctions.put(prop, restrictions);
				}
				restrictions.add(propRest);
			}
		}
		return propToRestirctions;
	}

	
	static class AssociationKey {
		Classifier src;
		String end1;
		Classifier trgt;
		String end2;
		
		public AssociationKey(Classifier src, String end1, Classifier trg, String end2) {
			this.src = src;
			this.trgt = trg;
			this.end1 = end1;
			this.end2 = end2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((end1 == null) ? 0 : end1.hashCode());
			result = prime * result + ((end2 == null) ? 0 : end2.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			result = prime * result + ((trgt == null) ? 0 : trgt.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AssociationKey other = (AssociationKey) obj;
			if (end1 == null) {
				if (other.end1 != null)
					return false;
			} else if (!end1.equals(other.end1))
				return false;
			if (end2 == null) {
				if (other.end2 != null)
					return false;
			} else if (!end2.equals(other.end2))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			if (trgt == null) {
				if (other.trgt != null)
					return false;
			} else if (!trgt.equals(other.trgt))
				return false;
			return true;
		}
	}
	
	private void createAssociation(Classifier srcClass, Classifier trgClass, String end1Name, boolean end1Navigable,
			String end2Name, boolean end2Navigable, int end1Lower, int end1Upper, int end2Lower, int end2Upper, String srcRelationIri, String targetRelationIri) {
		// if it is there it means it was handled at creation time
		trgClass.createAssociation(end1Navigable, AggregationKind.NONE_LITERAL, end1Name, end1Lower, end1Upper,
				srcClass, end2Navigable, AggregationKind.NONE_LITERAL, end2Name, end2Lower, end2Upper);
		if (!srcRelationIri.isBlank() && end1Navigable) {
			Property att1 = trgClass.getAttribute(end1Name, srcClass);
			UmlUtils.addNameAnnotationIfNeeded(att1);
			UmlUtils.addIRIAnnotation(att1, srcRelationIri);
		}
		if (!targetRelationIri.isBlank() && end2Navigable) {
			Property att2 = srcClass.getAttribute(end2Name, trgClass);
			UmlUtils.addNameAnnotationIfNeeded(att2);
			UmlUtils.addIRIAnnotation(att2, targetRelationIri);
		}
	}
	
	private static class AssociationInfo {
		public Class srcClass;
		public String end1Name = "";
		public boolean end1Navigable = true;
		public String end2Name = "";
		public boolean end2Navigable = false;
		public int end1Lower = 0;
		public int end1Upper = -1;
		public int end2Lower = 0;
		public int end2Upper = -1;
		public String srcIri = "";
		public String trgtIri = "";
		
		public AssociationInfo(Class srcClass, String end1Name, boolean end1Navigable, int end1Lower, int end1Upper,
							   String end2Name, boolean end2NAvigable, int end2Lower, int end2Upper, String srcIri, String trgtIri) {
			this.srcClass = srcClass;
			this.end1Name = end1Name;
			this.end1Navigable = end1Navigable;
			this.end1Lower = end1Lower;
			this.end1Upper = end1Upper;
			this.end2Name = end2Name;
			this.end2Navigable = end2NAvigable;
			this.end2Lower = end2Lower;
			this.end2Upper = end2Upper;
			this.srcIri = srcIri;
			this.trgtIri = trgtIri;
		}
		
		@Override
		public String toString() {
			return "Relation Entity for : " + srcClass.getName();
		}
	}

	private StereoTypesInfo getStereoTypeInfo(Vocabulary voc, Entity entity) {
		StereoTypesInfo infoHolder = new StereoTypesInfo(voc, entity);
		
		//List<Literal> values = OmlSearch.findAnnotationValuesForIri(entity, IS_STEREOTYPE_OF);
		List<SpecializableTerm> generals = OmlSearch.findSuperTerms(entity);
		for (SpecializableTerm general : generals) {
			if (general.getOwningVocabulary().getIri().equals(UmlUtils.UML_IRI)) {
				String metaclass = general.getName();
				if (metaclass.endsWith(CONCEPT_POSTFIX)) {
					metaclass = metaclass.substring(0, metaclass.length() - CONCEPT_POSTFIX.length());
				} else if (metaclass.endsWith(RELATION_POSTFIX)) {
					metaclass = metaclass.substring(0, metaclass.length() - RELATION_POSTFIX.length());
				}
				infoHolder.addMetaClass(metaclass);
			}
		}
		return infoHolder;
	}

	private DataType getTypeForRange(Scalar range) {
		// TODO : move to the correct API => Scalar javaScalar = OmlSearch.findJavaScalar(range)
		// TODO : remove the case "date" after the project oml file is updated to remove this scalar
		if (range instanceof FacetedScalar) {
			switch (range.getName()) {
			case "string":
			case "dateTime":
			case "decimal":
			case "date":
				return (PrimitiveType) umlMetaModel.getMember("String");
			case "integer":	
				return (PrimitiveType) umlMetaModel.getMember("Integer");
			case "double":
			case "rational":
			case "float":
			case "real":
				return (PrimitiveType) umlMetaModel.getMember("Real");
			case "boolean":
				return (PrimitiveType) umlMetaModel.getMember("Boolean");
			}
		} else if (range instanceof EnumeratedScalar || 
				   range instanceof Structure || 
				   range instanceof StructuredProperty ) {
			DataType foundType =  (DataType) converted.get(range);
			if (foundType==null) {
				logger.error("UnMapped Type: " + range.getName());
			}
			return foundType;
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
