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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import io.opencaesar.oml.AnnotatedElement;
import io.opencaesar.oml.Annotation;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.ConceptTypeAssertion;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.EnumeratedScalar;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.Instance;
import io.opencaesar.oml.LinkAssertion;
import io.opencaesar.oml.NamedInstance;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.RelationPredicate;
import io.opencaesar.oml.RelationTypeAssertion;
import io.opencaesar.oml.Rule;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.ScalarPropertyValueAssertion;
import io.opencaesar.oml.TypeAssertion;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml.util.OmlSwitch;
import io.opencaesar.oml2papyrus.util.UmlUtils;

public class DescriptionBundleToModel {

	private final DescriptionBundle rootOntology;
	private final Profile profile;
	private final File outputFolder;
	private final ResourceSet outputResourceSet;
	private final Logger logger;
	private final DescriptionBundleVisitor visitor; 
	
	private EObject resourceRoot;
	private Map<String, Type> iriToTypeMap;
	private final Map<io.opencaesar.oml.Element, EObject> oml2EcoreMap = new HashMap<>();
	private Set<RelationInstance> relations = new HashSet<>();
	private Set<LinkAssertion> links = new HashSet<>();
	private final boolean forceReifiedLinks;
	private Vocabulary umlVoc;
	
	public DescriptionBundleToModel(DescriptionBundle rootOntology, Profile profile, File outputFolder, boolean forceReifiedLinks, ResourceSet outputResourceSet, Logger logger) {
		this.rootOntology = rootOntology;
		this.profile = profile;
		this.outputFolder = outputFolder;
		this.forceReifiedLinks = forceReifiedLinks;
		this.outputResourceSet = outputResourceSet;
		this.logger = logger;
		this.visitor = createDescriptionBundleVisitor(rootOntology);
		this.iriToTypeMap = new HashMap<>();
		populateIriToTypeMap(profile, iriToTypeMap);
	}

	public Resource convert() throws Exception {
		// Create parent folder
		URI iri = URI.createURI(rootOntology.getIri());
		String authority = iri.authority();
		List<String> segments = iri.segmentsList();
		File parentFolder = new File(outputFolder.getPath()+File.separator+authority+File.separator+String.join(File.separator, segments.subList(0,  segments.size()-1)));
		parentFolder.mkdirs();
		
		// Create resource
		URI outputResourceUri = URI.createFileURI(parentFolder.getAbsolutePath()+File.separator+iri.lastSegment()+'.'+getOutputFileExtension());
		Resource outputResource = outputResourceSet.createResource(outputResourceUri);
		if (outputResource != null) {
			logger.info(outputResourceUri+" was created");
		} else {
			return null;
		}

		// Create resource root
		resourceRoot = createResourceRoot();
		if (resourceRoot != null) {
			outputResource.getContents().add(resourceRoot);
		}
		final Vocabulary[] umlVoc = new Vocabulary[1];
		// Collect all descriptions
		List<Description> allDescriptions = OmlRead.getAllImportedOntologies(rootOntology, false).stream().
			filter(o -> {
				if (o.getIri().equals(UmlUtils.UML_IRI)) {
					umlVoc[0] = (Vocabulary)o;
				}
				return o instanceof Description;
			}).
			map(o -> (Description)o).
			collect(Collectors.toList());
		this.umlVoc = umlVoc[0];
		// Convert each description
		allDescriptions.forEach(d -> visitor.doSwitch(d));
		logger.info("Converting Relations:");
		relations.forEach(r -> convertRelationInstance(r));
		logger.info("Converting Links:");
		links.forEach(l -> convertLink(l));
		return outputResource;
	}
	
	private Entity getUMLEntityByName(String name) {
		String iri = UmlUtils.UML_IRI + "/" + name;
		return (Entity)OmlRead.getMemberByIri(umlVoc, iri);
	}
	
	private Rule getUMLRuleByName(String name) {
		String iri = UmlUtils.UML_IRI + "/" + name;
		return (Rule)OmlRead.getMemberByIri(umlVoc, iri);
	}

	protected String getOutputFileExtension() {
		return UMLResource.FILE_EXTENSION;
	}
	
	protected EObject createResourceRoot() {
		URI iri = URI.createURI(rootOntology.getIri());
		Model model = UmlUtils.createModel(iri.lastSegment(), rootOntology.getIri());
		model.applyProfile(profile);
		return model;
	}

	protected DescriptionBundleVisitor createDescriptionBundleVisitor(DescriptionBundle descriptionBundle) {
		return new DescriptionBundleVisitor();
	}
	
	protected void populateIriToTypeMap(org.eclipse.uml2.uml.Package package_, Map<String, Type> map) {
		String uri = package_.getURI();
		for (Type type : package_.getOwnedTypes()) {
			map.put(uri+'#'+type.getName(), type);
		}
		package_.getNestedPackages().forEach(p -> populateIriToTypeMap(p, map));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public EObject convertScalarPropertyValueAssertion(ScalarPropertyValueAssertion object) {
		Instance instance = OmlRead.getSubject(object);
		ScalarProperty property = object.getProperty();
		Object value = OmlSearch.findJavaValue(object.getValue());
		if (object.getProperty().getRange() instanceof EnumeratedScalar) {
			value = UmlUtils.getUMLFirendlyName(value.toString()); 
		}
		if (instance instanceof NamedInstance) {
			Element element = (Element) oml2EcoreMap.get(instance);
			Stereotype stereotype = (Stereotype) iriToTypeMap.get(property.getDomain().getIri());
			List<Stereotype> stereotypes = element.getAppliedSubstereotypes(stereotype);
			for (Stereotype s : stereotypes) {
				// check for many
				Object val = element.getValue(s, UmlUtils.getUMLFirendlyName(property.getName()));
				if (val instanceof List) {
					//ugly
					((List)val).add(value);
				} else {
					element.setValue(s, property.getName(), value);
				}
			}
		} else {
			EObject element = oml2EcoreMap.get(instance);
			EStructuralFeature feature = element.eClass().getEStructuralFeature(property.getName());
			if (feature.isMany()) {
				((List)element.eGet(feature,true)).add(value);
			}else  {
				element.eSet(feature, value);
			}
		}
		return null;
	}
	
	public void convertRelationInstance(RelationInstance object) {
		List<RelationTypeAssertion> assertions = OmlSearch.findTypeAssertions(object);
		if (assertions.isEmpty()) {
			throw new IllegalArgumentException("relation instance "+object.getIri()+" does not have a type");
		}
		RelationEntity relationEntity = assertions.get(0).getType();

		Stereotype stereotype = (Stereotype) iriToTypeMap.get(relationEntity.getIri());
		if (stereotype == null) {
			throw new IllegalArgumentException("stereotype "+relationEntity.getIri()+" is not found in the profile");
		}

		List<org.eclipse.uml2.uml.Class> metaclasses = stereotype.getAllExtendedMetaclasses();
		if (metaclasses.isEmpty()) {
			throw new IllegalArgumentException("stereotype "+relationEntity.getIri()+" does not extend any metaclass");
		}
		EClass eClass = (EClass) UMLPackage.eINSTANCE.getEClassifier(metaclasses.get(0).getName());
		
		PackageableElement element = (PackageableElement) UMLFactory.eINSTANCE.create(eClass) ;
		element.setName(object.getName());
		UmlUtils.addNameAnnotationIfNeeded(element);
		oml2EcoreMap.put(object, element);
		List<NamedElement> sources = object.getSources().stream().map(s -> (NamedElement) oml2EcoreMap.get(s)).collect(Collectors.toList());
		List<NamedElement> targets = object.getTargets().stream().map(s -> (NamedElement) oml2EcoreMap.get(s)).collect(Collectors.toList());
		Entity entity = getUMLEntityByName(eClass.getName());
		if (entity instanceof RelationEntity) {
			Rule rule = getUMLRuleByName(eClass.getName()+"_Rule");
			Relation sourceRel = ((RelationPredicate)rule.getConsequent().get(0)).getRelation();
			String sourceName = getNamefromAnnotation(sourceRel);
			Relation targetRel = ((RelationPredicate)rule.getConsequent().get(1)).getRelation();
			String targetName = getNamefromAnnotation(targetRel);
			EStructuralFeature sourceFeature = element.eClass().getEStructuralFeature(sourceName);
			EStructuralFeature targetFeature = element.eClass().getEStructuralFeature(targetName);
			if (sourceFeature==null || targetFeature==null) {
				logger.error("Error: converting relation");
			}
			setFeatureValue(sourceFeature,element,sources);
			setFeatureValue(targetFeature,element,targets);
		}else if (eClass.getClassifierID() == UMLPackage.ASSOCIATION) {
			setAssociationDetails((Association)element,sources, targets);
		}

		org.eclipse.uml2.uml.Package package_ = (org.eclipse.uml2.uml.Package) oml2EcoreMap.get(object.getOntology());
		package_.getPackagedElements().add(element);
		
		for (RelationEntity aRelationEntity : assertions.stream().map(a -> a.getType()).collect(Collectors.toSet())) {
			Stereotype aStereotype = (Stereotype) iriToTypeMap.get(aRelationEntity.getIri());
			if (aStereotype == null) {
				throw new IllegalArgumentException("stereotype "+aRelationEntity.getIri()+" is not found in the profile");
			}
			element.applyStereotype(aStereotype);
		}
		OmlSearch.findPropertyValueAssertions(object).forEach(e ->{ 
				if (e instanceof ScalarPropertyValueAssertion) {
					convertScalarPropertyValueAssertion((ScalarPropertyValueAssertion)e);
				}
			});
	}

	private void setAssociationDetails(Association association, List<NamedElement> sources, List<NamedElement> targets) {
		//TODO: annotation to provide info for the system about the aggregation kind and and who owns the navigable end is it the association or the source
		List<Property> props = new ArrayList<>();
		for (NamedElement sourceType : sources) {
			Property prop = UMLFactory.eINSTANCE.createProperty();
			prop.setAggregation(AggregationKind.NONE_LITERAL);
			prop.setType((Type)sourceType);
			association.getOwnedEnds().add(prop);
			props.add(prop);
		}
		for (NamedElement targetType : targets) {
			Property prop = UMLFactory.eINSTANCE.createProperty();
			prop.setAggregation(AggregationKind.NONE_LITERAL);
			prop.setType((Type)targetType);
			association.getNavigableOwnedEnds().add(prop);
			//prop.setIsNavigable(targets.size()==1);
			props.add(prop);
		}
		association.getMemberEnds().addAll(props);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setFeatureValue(EStructuralFeature feature, PackageableElement element,
			List<NamedElement> sources) {
		if (feature.isMany()) {
			((List)element.eGet(feature,true)).addAll(sources);
		}else  {
			element.eSet(feature, sources.get(0));
		}
	}

	private String getNamefromAnnotation(AnnotatedElement element) {
		List<Annotation> annotations = OmlSearch.findAnnotations(element);
		for (Annotation annotation : annotations) {
			if (annotation.getProperty().getIri().equals(OmlConstants.DC_NS+"title")) {
				String val = OmlRead.getStringValue(annotation.getValue());
				return val;
			}
		}
		return "";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void convertLink(LinkAssertion object) {
		Relation relation = object.getRelation();
		RelationEntity relationEntity = relation.getRelationEntity();
		NamedInstance sourceInst = OmlRead.getSource(object);
		NamedInstance targetInst = object.getTarget();
		
		Stereotype stereotype = (Stereotype) iriToTypeMap.get(relationEntity.getIri());
		if (stereotype == null) {
			throw new IllegalArgumentException("stereotype "+relationEntity.getIri()+" is not found in the profile");
		}

		List<org.eclipse.uml2.uml.Class> metaclasses = stereotype.getAllExtendedMetaclasses();
		if (metaclasses.isEmpty()) {
			throw new IllegalArgumentException("stereotype "+relationEntity.getIri()+" does not extend any metaclass");
		}
		
		// reified flow should be triggered if the reified flag is ON
		EClass eClass = (EClass) UMLPackage.eINSTANCE.getEClassifier(metaclasses.get(0).getName());
		
		if (forceReifiedLinks) {
			PackageableElement element = (PackageableElement) UMLFactory.eINSTANCE.create(eClass) ;
			element.setName(null);
			oml2EcoreMap.put(object, element);

			NamedElement source = (relation instanceof ForwardRelation) ?
					(NamedElement) oml2EcoreMap.get(sourceInst) :
					(NamedElement) oml2EcoreMap.get(targetInst);
			NamedElement target = (relation instanceof ForwardRelation) ?
					(NamedElement) oml2EcoreMap.get(targetInst) :
						(NamedElement) oml2EcoreMap.get(sourceInst);

			org.eclipse.uml2.uml.Package package_ = (Package) oml2EcoreMap.get(object.getOntology());
			package_.getPackagedElements().add(element);
			Entity entity = getUMLEntityByName(eClass.getName());
			if (entity instanceof RelationEntity) {
				Rule rule = getUMLRuleByName(eClass.getName()+"_Rule");
				Relation sourceRel = ((RelationPredicate)rule.getConsequent().get(0)).getRelation();
				String sourceName = getNamefromAnnotation(sourceRel);
				Relation targetRel = ((RelationPredicate)rule.getConsequent().get(1)).getRelation();
				String targetName = getNamefromAnnotation(targetRel);
				EStructuralFeature sourceFeature = element.eClass().getEStructuralFeature(sourceName);
				EStructuralFeature targetFeature = element.eClass().getEStructuralFeature(targetName);
				setFeatureValue(sourceFeature,element,Collections.singletonList(source));
				setFeatureValue(targetFeature,element,Collections.singletonList(target));
			}else if (eClass.getClassifierID() == UMLPackage.ASSOCIATION) {
				setAssociationDetails((Association)element,Collections.singletonList(source), Collections.singletonList(target));
			}
			element.applyStereotype(stereotype);
		}else {
			NamedElement source = (NamedElement) oml2EcoreMap.get(sourceInst);
			NamedElement target = (NamedElement) oml2EcoreMap.get(targetInst);
			EObject sourceApplication = getStereoTypeApplication(sourceInst, source);
			EObject targetApplication = getStereoTypeApplication(targetInst, target);
			if (targetApplication==null) {
				logger.error("could not find stereotype application for link " + target);
			}
			String relName = object.getRelation().getName();
			EStructuralFeature feature = sourceApplication.eClass().getEStructuralFeature(relName);
			if (feature.isMany()) {
				((List)sourceApplication.eGet(feature,true)).add(targetApplication);
			}else  {
				sourceApplication.eSet(feature, targetApplication);
			}
		}
	}
	
	private EObject getStereoTypeApplication(NamedInstance instance, NamedElement element ) {
		List<TypeAssertion> assertions = OmlSearch.findTypeAssertions(instance);
		Entity type = assertions.get(0).getType();
		Stereotype stereotype = (Stereotype) iriToTypeMap.get(type.getIri());
		return  element.getStereotypeApplication(stereotype);
	}

	public class DescriptionBundleVisitor extends OmlSwitch<EObject> {

		@Override
		protected EObject doSwitch(int classifierID, EObject theEObject) {
			EObject result = oml2EcoreMap.get(theEObject);
			if (result == null) {
				if (links.contains(theEObject) || relations.contains(theEObject)) {
					return theEObject;
				}
				result = super.doSwitch(classifierID, theEObject);
			}
			return result;
		}

		@Override
		public EObject caseDescription(Description object) {
			org.eclipse.uml2.uml.Package package_ = UmlUtils.getPackage(object.getIri(), (Model)resourceRoot);
			oml2EcoreMap.put(object, package_);
			object.getOwnedStatements().forEach(e -> doSwitch(e));
			return package_;
		}
		
		@Override
		public EObject caseConceptInstance(ConceptInstance object) {
			List<ConceptTypeAssertion> assertions = OmlSearch.findTypeAssertions(object);
			if (assertions.isEmpty()) {
				throw new IllegalArgumentException("concept instance "+object.getIri()+" does not have a type");
			}
			Concept concept = assertions.get(0).getType();
			
			Stereotype stereotype = (Stereotype) iriToTypeMap.get(concept.getIri());
			if (stereotype == null) {
				throw new IllegalArgumentException("stereotype "+concept.getIri()+" is not found in the profile");
			}

			List<org.eclipse.uml2.uml.Class> metaclasses = stereotype.getAllExtendedMetaclasses();
			if (metaclasses.isEmpty()) {
				throw new IllegalArgumentException("stereotype "+concept.getIri()+" does not extend any metaclass");
			}
			EClass eClass = (EClass) UMLPackage.eINSTANCE.getEClassifier(UmlUtils.getUMLFirendlyName(metaclasses.get(0).getName()));
			
			PackageableElement element = (PackageableElement) UMLFactory.eINSTANCE.create(eClass) ;
			element.setName(UmlUtils.getUMLFirendlyName(object.getName()));
			oml2EcoreMap.put(object, element);

			org.eclipse.uml2.uml.Package package_ = (org.eclipse.uml2.uml.Package) doSwitch(object.getOntology());
			package_.getPackagedElements().add(element);

			for (Concept aConcept : assertions.stream().map(a -> a.getType()).collect(Collectors.toSet())) {
				Stereotype aStereotype = (Stereotype) iriToTypeMap.get(aConcept.getIri());
				if (aStereotype == null) {
					throw new IllegalArgumentException("stereotype "+aConcept.getIri()+" is not found in the profile");
				}
				element.applyStereotype(aStereotype);
			}
			
			OmlSearch.findLinkAssertions(object).forEach(e -> doSwitch(e));
			OmlSearch.findPropertyValueAssertions(object).forEach(e -> doSwitch(e));
			
			return element;
		}

		@Override
		public EObject caseRelationInstance(RelationInstance object) {
			relations.add(object);			
			OmlSearch.findLinkAssertions(object).forEach(e -> links.add(e));
			return object;
		}
		
		public EObject caseLinkAssertion(LinkAssertion object) {
			links.add(object);
			return object;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public EObject caseScalarPropertyValueAssertion(ScalarPropertyValueAssertion object) {
			Object value = OmlSearch.findJavaValue(object.getValue());
			if (object.getProperty().getRange() instanceof EnumeratedScalar) {
				value = UmlUtils.getUMLFirendlyName(value.toString()); 
			}
			ScalarProperty property = object.getProperty();
			Instance instance = OmlRead.getSubject(object);
			if (instance instanceof NamedInstance) {
				Element element = (Element) doSwitch(instance);
				Stereotype stereotype = (Stereotype) iriToTypeMap.get(object.getProperty().getDomain().getIri());
				List<Stereotype> stereotypes = element.getAppliedSubstereotypes(stereotype);
				for (Stereotype s : stereotypes) {
					// check for many
					Object val = element.getValue(s, UmlUtils.getUMLFirendlyName(property.getName()));
					if (val instanceof List) {
						//ugly
						((List)val).add(value);
					} else {
						element.setValue(s, property.getName(), value);
					}
				}
			} else {
				EObject element = doSwitch(instance);
				EStructuralFeature feature = element.eClass().getEStructuralFeature(property.getName());
				if (feature.isMany()) {
					((List)element.eGet(feature,true)).add(value);
				}else  {
					element.eSet(feature, value);
				}
			}
			return null;
		}
	}
}
