package io.opencaesar.oml2papyrus;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.DirectedRelationship;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import io.opencaesar.oml.Concept;
import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.ConceptTypeAssertion;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.Instance;
import io.opencaesar.oml.LinkAssertion;
import io.opencaesar.oml.NamedInstance;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.RelationTypeAssertion;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.ScalarPropertyValueAssertion;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml.util.OmlVisitor;
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
	
	public DescriptionBundleToModel(DescriptionBundle rootOntology, Profile profile, File outputFolder, ResourceSet outputResourceSet, Logger logger) {
		this.rootOntology = rootOntology;
		this.profile = profile;
		this.outputFolder = outputFolder;
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
			
		// Collect all descriptions
		List<Description> allDescriptions = OmlRead.getAllImportedOntologies(rootOntology).stream().
			filter(o -> o instanceof Description).
			map(o -> (Description)o).
			collect(Collectors.toList());
		
		// Convert each description
		allDescriptions.forEach(d -> visitor.doSwitch(d));
		
		return outputResource;
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

	public class DescriptionBundleVisitor extends OmlVisitor<EObject> {

		private final Map<io.opencaesar.oml.Element, EObject> oml2EcoreMap = new HashMap<>();
		
		@Override
		protected EObject doSwitch(int classifierID, EObject theEObject) {
			EObject result = oml2EcoreMap.get(theEObject);
			if (!oml2EcoreMap.containsKey(theEObject)) {
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
				throw new IllegalArgumentException("concept instance "+OmlRead.getIri(object)+" does not have a type");
			}
			Concept concept = assertions.get(0).getType();
			
			Stereotype stereotype = (Stereotype) iriToTypeMap.get(OmlRead.getIri(concept));
			if (stereotype == null) {
				throw new IllegalArgumentException("stereotype "+OmlRead.getIri(concept)+" is not found in the profile");
			}

			List<org.eclipse.uml2.uml.Class> metaclasses = stereotype.getAllExtendedMetaclasses();
			if (metaclasses.isEmpty()) {
				throw new IllegalArgumentException("stereotype "+OmlRead.getIri(concept)+" does not extend any metaclass");
			}
			EClass eClass = (EClass) UMLPackage.eINSTANCE.getEClassifier(metaclasses.get(0).getName());
			
			org.eclipse.uml2.uml.Package package_ = (org.eclipse.uml2.uml.Package) doSwitch(OmlRead.getOntology(object));
			Element element = package_.createPackagedElement(object.getName(), eClass);
			oml2EcoreMap.put(object, element);
			
			for (Concept aConcept : assertions.stream().map(a -> a.getType()).collect(Collectors.toSet())) {
				Stereotype aStereotype = (Stereotype) iriToTypeMap.get(OmlRead.getIri(aConcept));
				if (aStereotype == null) {
					throw new IllegalArgumentException("stereotype "+OmlRead.getIri(aConcept)+" is not found in the profile");
				}
				element.applyStereotype(aStereotype);
			}
			
			OmlSearch.findLinkAssertionsWithSource(object).forEach(e -> doSwitch(e));
			OmlSearch.findPropertyValueAssertions(object).forEach(e -> doSwitch(e));
			
			return element;
		}

		@Override
		public EObject caseRelationInstance(RelationInstance object) {
			List<RelationTypeAssertion> assertions = OmlSearch.findTypeAssertions(object);
			if (assertions.isEmpty()) {
				throw new IllegalArgumentException("relation instance "+OmlRead.getIri(object)+" does not have a type");
			}
			RelationEntity relationEntity = assertions.get(0).getType();

			Stereotype stereotype = (Stereotype) iriToTypeMap.get(OmlRead.getIri(relationEntity));
			if (stereotype == null) {
				throw new IllegalArgumentException("stereotype "+OmlRead.getIri(relationEntity)+" is not found in the profile");
			}

			List<org.eclipse.uml2.uml.Class> metaclasses = stereotype.getAllExtendedMetaclasses();
			if (metaclasses.isEmpty()) {
				throw new IllegalArgumentException("stereotype "+OmlRead.getIri(relationEntity)+" does not extend any metaclass");
			}
			EClass eClass = (EClass) UMLPackage.eINSTANCE.getEClassifier(metaclasses.get(0).getName());
			if (!UMLPackage.Literals.DIRECTED_RELATIONSHIP.isSuperTypeOf(eClass)) {
				throw new IllegalArgumentException("stereotype "+OmlRead.getIri(relationEntity)+" extends metaclass "+eClass.getName()+" which is not a directed relationship" );
			}
			
			org.eclipse.uml2.uml.Package package_ = (org.eclipse.uml2.uml.Package) doSwitch(OmlRead.getOntology(object));
			DirectedRelationship relationship = (DirectedRelationship) package_.createPackagedElement(object.getName(), eClass);
			oml2EcoreMap.put(object, relationship);
	
			List<NamedElement> sources = object.getSources().stream().map(s -> (NamedElement) doSwitch(s)).collect(Collectors.toList());
			List<NamedElement> targets = object.getTargets().stream().map(s -> (NamedElement) doSwitch(s)).collect(Collectors.toList());
			UmlUtils.setSources(relationship, sources);
			UmlUtils.setTargets(relationship, targets);
			
			for (RelationEntity aRelationEntity : assertions.stream().map(a -> a.getType()).collect(Collectors.toSet())) {
				Stereotype aStereotype = (Stereotype) iriToTypeMap.get(OmlRead.getIri(aRelationEntity));
				if (aStereotype == null) {
					throw new IllegalArgumentException("stereotype "+OmlRead.getIri(aRelationEntity)+" is not found in the profile");
				}
				relationship.applyStereotype(aStereotype);
			}
			
			OmlSearch.findLinkAssertionsWithSource(object).forEach(e -> doSwitch(e));
			OmlSearch.findPropertyValueAssertions(object).forEach(e -> doSwitch(e));

			return relationship;
		}
		
		@Override
		public EObject caseLinkAssertion(LinkAssertion object) {
			RelationEntity relationEntity = OmlRead.getRelationEntity(object.getRelation());

			Stereotype stereotype = (Stereotype) iriToTypeMap.get(OmlRead.getIri(relationEntity));
			if (stereotype == null) {
				throw new IllegalArgumentException("stereotype "+OmlRead.getIri(relationEntity)+" is not found in the profile");
			}

			List<org.eclipse.uml2.uml.Class> metaclasses = stereotype.getAllExtendedMetaclasses();
			if (metaclasses.isEmpty()) {
				throw new IllegalArgumentException("stereotype "+OmlRead.getIri(relationEntity)+" does not extend any metaclass");
			}
			EClass eClass = (EClass) UMLPackage.eINSTANCE.getEClassifier(metaclasses.get(0).getName());
			if (!UMLPackage.Literals.DIRECTED_RELATIONSHIP.isSuperTypeOf(eClass)) {
				throw new IllegalArgumentException("stereotype "+OmlRead.getIri(relationEntity)+" extends metaclass "+eClass.getName()+" which is not a directed relationship" );
			}
			
			org.eclipse.uml2.uml.Package package_ = (org.eclipse.uml2.uml.Package) doSwitch(OmlRead.getOntology(object));
			DirectedRelationship relationship = (DirectedRelationship) package_.createPackagedElement(null, eClass);
			oml2EcoreMap.put(object, relationship);
	
			NamedElement source = (NamedElement) doSwitch(OmlRead.getSource(object));
			NamedElement target = (NamedElement) doSwitch(object.getTarget());
			if (object.getRelation() instanceof ForwardRelation) {
				UmlUtils.setSources(relationship, Collections.singletonList(source));
				UmlUtils.setTargets(relationship, Collections.singletonList(target));
			} else {
				UmlUtils.setSources(relationship, Collections.singletonList(target));
				UmlUtils.setTargets(relationship, Collections.singletonList(source));
			}
			
			relationship.applyStereotype(stereotype);
			
			return relationship;
		}

		@Override
		public EObject caseScalarPropertyValueAssertion(ScalarPropertyValueAssertion object) {
			Object value = OmlSearch.findTypedLiteralValue(object.getValue());
			ScalarProperty property = object.getProperty();
			Instance instance = OmlRead.getInstance(object);
			if (instance instanceof NamedInstance) {
				Element element = (Element) doSwitch(instance);
				Stereotype stereotype = (Stereotype) iriToTypeMap.get(OmlRead.getIri(object.getProperty().getDomain()));
				List<Stereotype> stereotypes = element.getAppliedSubstereotypes(stereotype);
				for (Stereotype s : stereotypes) {
					element.setValue(s, property.getName(), value);
				}
			} else {
				EObject element = doSwitch(instance);
				EStructuralFeature feature = element.eClass().getEStructuralFeature(property.getName());
				element.eSet(feature, value);
			}
			return null;
		}
	}
}
