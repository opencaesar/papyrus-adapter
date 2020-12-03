package io.opencaesar.oml2papyrus;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Dependency;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.LinkAssertion;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml.util.OmlVisitor;
import io.opencaesar.oml2papyrus.util.UmlUtils;

public class DescriptionBundleToModel {

	private final DescriptionBundle rootOntology;
	private final File outputFolder;
	private final ResourceSet outputResourceSet;
	private final Logger logger;
	private final DescriptionBundleVisitor visitor; 
	
	private EObject resourceRoot;
	
	public DescriptionBundleToModel(DescriptionBundle rootOntology, File outputFolder, ResourceSet outputResourceSet, Logger logger) {
		this.rootOntology = rootOntology;
		this.outputFolder = outputFolder;
		this.outputResourceSet = outputResourceSet;
		this.logger = logger;
		this.visitor = createDescriptionBundleVisitor();
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
		return UmlUtils.createModel(iri.lastSegment(), rootOntology.getIri());
	}

	protected DescriptionBundleVisitor createDescriptionBundleVisitor() {
		return new DescriptionBundleVisitor();
	}
	
	public class DescriptionBundleVisitor extends OmlVisitor<Element> {

		private final Map<io.opencaesar.oml.Element, Element> oml2UmlMap = new HashMap<>();
		
		@Override
		protected Element doSwitch(int classifierID, EObject theEObject) {
			Element result = oml2UmlMap.get(theEObject);
			if (!oml2UmlMap.containsKey(theEObject)) {
				result = super.doSwitch(classifierID, theEObject);
			}
			return result;
		}

		@Override
		public Element caseDescription(Description object) {
			org.eclipse.uml2.uml.Package package_ = UmlUtils.getPackage(object.getIri(), (Model)resourceRoot);
			oml2UmlMap.put(object, package_);
			object.getOwnedStatements().forEach(e -> doSwitch(e));
			return package_;
		}
		
		@Override
		public Element caseConceptInstance(ConceptInstance object) {
			org.eclipse.uml2.uml.Package package_ = (org.eclipse.uml2.uml.Package) doSwitch(OmlRead.getOntology(object));
			Class clazz = package_.createOwnedClass(object.getName(), false);
			oml2UmlMap.put(object, clazz);
			OmlSearch.findLinkAssertionsWithSource(object).forEach(e -> doSwitch(e));
			OmlSearch.findPropertyValueAssertions(object).forEach(e -> doSwitch(e));
			return clazz;
		}

		@Override
		public Element caseRelationInstance(RelationInstance object) {
			org.eclipse.uml2.uml.Package package_ = (org.eclipse.uml2.uml.Package) doSwitch(OmlRead.getOntology(object));
			Dependency dependency = (Dependency) package_.createPackagedElement(null, UMLPackage.Literals.DEPENDENCY);
			oml2UmlMap.put(object, dependency);
			List<NamedElement> sources = object.getSources().stream().map(s -> (NamedElement) doSwitch(s)).collect(Collectors.toList());
			List<NamedElement> targets = object.getTargets().stream().map(s -> (NamedElement) doSwitch(s)).collect(Collectors.toList());
			dependency.getClients().addAll(sources);
			dependency.getSuppliers().addAll(targets);
			OmlSearch.findLinkAssertionsWithSource(object).forEach(e -> doSwitch(e));
			OmlSearch.findPropertyValueAssertions(object).forEach(e -> doSwitch(e));
			return dependency;
		}
		
		@Override
		public Element caseLinkAssertion(LinkAssertion object) {
			org.eclipse.uml2.uml.Package package_ = (org.eclipse.uml2.uml.Package) doSwitch(OmlRead.getOntology(object));
			Dependency dependency = (Dependency) package_.createPackagedElement(OmlRead.getRelationEntity(object.getRelation()).getName(), UMLPackage.Literals.DEPENDENCY);
			oml2UmlMap.put(object, dependency);
			NamedElement source = (NamedElement) doSwitch(OmlRead.getSource(object));
			NamedElement target = (NamedElement) doSwitch(object.getTarget());
			if (object.getRelation() instanceof ReverseRelation) {
				dependency.getClients().add(target);
				dependency.getSuppliers().add(source);
			} else {
				dependency.getClients().add(source);
				dependency.getSuppliers().add(target);
			}
			return dependency;
		}

	}
}
