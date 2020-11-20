package io.opencaesar.papyrus2oml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

import io.opencaesar.oml.Concept;
import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.SourceRelation;
import io.opencaesar.oml.TargetRelation;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class Ecore2OmlConverter {

	private static final String OML_EXTENSION = "oml";
	private static final String OML_XMI_EXTENSION = "omlxmi";

	private File inputFolder;
	private File inputModelFile;
	private ResourceSet omlResourceSet;
	private OmlCatalog catalog;
	private OmlWriter writer;
	private Logger logger;
	
	public Ecore2OmlConverter(File inputFolder, File inputModelFile, ResourceSet omlResourceSet, OmlCatalog catalog, OmlWriter writer, Logger logger) {
		this.inputFolder = inputFolder;
		this.inputModelFile = inputModelFile;
		this.omlResourceSet = omlResourceSet;
		this.catalog = catalog;
		this.writer = writer;
		this.logger = logger;
	}

	public List<Resource> convert() throws Exception {
		// load the input model file and check the root object
		// if the root is a profile, create a vocabulary budle
		// else create a description bundle
		return createDescriptionBundle(inputModelFile);
	}
	
	public List<Resource> createDescriptionBundle(File rootModelFile) throws Exception {
		// create input resource set
		ResourceSet inputResourceSet = createInputResourceSet();
		final List<Resource> resources = new ArrayList<>();

		// derive the description bundle prefix
		final String bundlePrefix = getDescriptionBundlePrefix(rootModelFile);
		
		// derive the description bundle IRI
		final String bundleIri = getDescriptionBundleIri(rootModelFile);
		
		// create the description bundle
		final URI descriptionBundleUri = URI.createURI(catalog.resolveURI(bundleIri)+"."+OML_EXTENSION);
		logger.info("Creating: "+descriptionBundleUri);
		DescriptionBundle bundle = writer.createDescriptionBundle(descriptionBundleUri, bundleIri, SeparatorKind.HASH, bundlePrefix);
		resources.add(bundle.eResource());
		
		// load root model file
		Resource r = inputResourceSet.getResource(URI.createFileURI(rootModelFile.getCanonicalPath()), true);
		
		// get all resource URIs to be converted
		List<URI> uris = getAllResourceURIs(r);
		
		//loop on all referenced UML models and convert them to descriptions
		for (URI uri : uris) {
			// get the resource
			Resource resource = inputResourceSet.getResource(uri, true);
			EcoreUtil.resolveAll(resource);
			
			// convert the file to description
			Description description = createDescription(resource);
			
			// add description to the bundle
			writer.addDescriptionBundleInclusion(bundle, description.getIri(), null);
			
			// add the description resources
			resources.add(description.eResource());
		}
		
		return resources;
	}

	protected Description createDescription(Resource resource) throws Exception {
		// derive the description prefix
		final String descriptionPrefix = getDescriptionPrefix(resource.getURI());
		
		// derive the description bundle IRI
		final String descriptionIri = getDescriptionIri(resource.getURI());
		
		// create a description
		final URI descriptionUri = URI.createURI(catalog.resolveURI(descriptionIri)+"."+OML_EXTENSION);
		logger.info("Creating: "+descriptionUri);
		Description description = writer.createDescription(descriptionUri, descriptionIri, SeparatorKind.HASH, descriptionPrefix);
		
		// populate the description based on model contents
		TreeIterator<EObject> i = resource.getAllContents();
		
		// get objects to convert
		List<EObject> objects = StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(i, Spliterator.ORDERED), false).
				filter(e -> shouldBeConverted(e)).
				collect(Collectors.toList());
		
		// convert the objects
		for (EObject eObject : objects) {
			EClass eClass = eObject.eClass();
			String vocabularyIri = eClass.getEPackage().getNsURI();
			URI vocabularyURI = getOntologyURI(vocabularyIri);
			if (vocabularyURI != null) {
				Resource vocabularyResource = omlResourceSet.getResource(vocabularyURI, true);
				Vocabulary vocabulary = (Vocabulary) OmlRead.getOntology(vocabularyResource);

				List<String> importedIris = description.getOwnedImports().stream().map(j -> j.getUri()).collect(Collectors.toList());
				if (!importedIris.contains(vocabularyIri)) {
					writer.addDescriptionUsage(description, vocabularyIri, null);
				}
				
				Entity entity = (Entity) OmlRead.getMemberByName(vocabulary, eClass.getName());
				if (entity instanceof Concept) {
					ConceptInstance instance = writer.addConceptInstance(description, getEObjectName(eObject));
					writer.addConceptTypeAssertion(description, OmlRead.getIri(instance), OmlRead.getIri(entity));
				} else if (entity instanceof RelationEntity) {
					RelationEntity re = (RelationEntity) entity;
					
					List<String> sourceIris = new ArrayList<>();
					String sourceEReferenceName = getSourceEReferenceName(re);
					if (sourceEReferenceName != null) {
						EReference sourceEReference = (EReference) eClass.getEStructuralFeature(sourceEReferenceName);
						if (sourceEReference.isMany()) {
							@SuppressWarnings("unchecked")
							List<EObject> sources = (List<EObject>) eObject.eGet(sourceEReference);
							for (EObject source : sources) {
								String sourceIri = getEObjectIri((EObject)source);
								if (sourceIri != null) {
									sourceIris.add(sourceIri);
								}
							}
						} else {
							Object source = eObject.eGet(sourceEReference);
							String sourceIri = getEObjectIri((EObject)source);
							if (sourceIri != null) {
								sourceIris.add(sourceIri);
							}
						}
					}
					
					List<String> targetIris = new ArrayList<>();
					String targetEReferenceName = getTargetEReferenceName(re);
					if (targetEReferenceName != null) {
						EReference targetEReference = (EReference) eClass.getEStructuralFeature(targetEReferenceName);
						if (targetEReference.isMany()) {
							@SuppressWarnings("unchecked")
							List<EObject> targets = (List<EObject>) eObject.eGet(targetEReference);
							for (EObject target : targets) {
								String targetIri = getEObjectIri((EObject)target);
								if (targetIri != null) {
									targetIris.add(targetIri);
								}
							}
						} else {
							Object target = eObject.eGet(targetEReference);
							String targetIri = getEObjectIri((EObject)target);
							if (targetIri != null) {
								targetIris.add(targetIri);
							}
						}
					}
					
					if (!sourceIris.isEmpty() && !targetIris.isEmpty()) {
						RelationInstance instance = writer.addRelationInstance(description, getEObjectName(eObject), sourceIris, targetIris);
						writer.addRelationTypeAssertion(description, OmlRead.getIri(instance), OmlRead.getIri(entity));
					}
				}
			}
		}
	
		return description;
	}
	
	protected ResourceSet createInputResourceSet() {
		return new ResourceSetImpl();
	}
	
	protected String getDescriptionBundlePrefix(File rootModelFile) {
		String fileName = rootModelFile.getName();
		return fileName.substring(0, fileName.lastIndexOf('.'))+"-bundle";
	}

	protected String getDescriptionBundleIri(File rootModelFile) {
		String relativePath = inputFolder.toPath().normalize().relativize(rootModelFile.toPath()).toString();
		return "http://"+relativePath.substring(0, relativePath.lastIndexOf('.'))+"-bundle";
	}
	
	protected String getDescriptionPrefix(URI uri) {
		String fileName = uri.lastSegment();
		return fileName.substring(0, fileName.lastIndexOf('.'));
	}

	protected String getDescriptionIri(URI uri) {
		String fileURI = uri.toFileString();
		if (fileURI != null) {
			String relativePath = inputFolder.toPath().normalize().relativize(Path.of(fileURI)).toString();
			return "http://"+relativePath.substring(0, relativePath.lastIndexOf('.'));
		}
		return null;
	}

	protected List<URI> getAllResourceURIs(Resource r) {
		return Collections.singletonList(r.getURI());
	}
	
	protected URI getOntologyURI(String iri) throws IOException {
		final URI baseIri = URI.createURI(catalog.resolveURI(iri));
		final URI ontologyUri;
		if (new File(baseIri.toFileString()+"."+OML_EXTENSION).exists()) {
			ontologyUri = URI.createURI(baseIri+"."+OML_EXTENSION);
		} else if (new File(baseIri.toFileString()+"."+OML_XMI_EXTENSION).exists()) {
			ontologyUri = URI.createURI(baseIri+"."+OML_XMI_EXTENSION);
		} else {
			throw new RuntimeException("Ontology with iri '"+"' cannot be found in the catalog");
		}
		return ontologyUri;
	}

	protected String getEObjectName(EObject eObject) {
		return eObject.eResource().getURIFragment(eObject);
	}
	
	protected String getEObjectIri(EObject eObject) {
		if (!eObject.eIsProxy()) {
			final String descriptionIri = getDescriptionIri(eObject.eResource().getURI());
			return (descriptionIri == null) ? null : descriptionIri + SeparatorKind.HASH + getEObjectName(eObject);
		}
		return null;
	}

	protected String getSourceEReferenceName(RelationEntity re) {
		SourceRelation sr = re.getSourceRelation();
		if (sr != null) {
			// We need to add the dc:title and rdfs:label annotations on source/target/inverseSource/inverseTarget as well
			// For now read them by parsing the relation name
			return sr.getName().split("_")[1];
		} else {
			RelationEntity superEntity = OmlRead.getSpecializedTerms(re).stream().
				filter(i -> i instanceof RelationEntity).
				map(i -> (RelationEntity)i).
				findAny().orElse(null);
			if (superEntity != null) {
				return getSourceEReferenceName(superEntity);
			}
			return null;
		}
	}

	protected String getTargetEReferenceName(RelationEntity re) {
		TargetRelation tr = re.getTargetRelation();
		if (tr != null) {
			// We need to add the dc:title and rdfs:label annotations on source/target/inverseSource/inverseTarget as well
			// For now read them by parsing the relation name
			return tr.getName().split("_")[1];
		} else {
			RelationEntity superEntity = OmlRead.getSpecializedTerms(re).stream().
				filter(i -> i instanceof RelationEntity).
				map(i -> (RelationEntity)i).
				findAny().orElse(null);
			if (superEntity != null) {
				return getSourceEReferenceName(superEntity);
			}
			return null;
		}
	}
	
	protected boolean shouldBeConverted(EObject eObject) {
		return true;
	}
	
}
