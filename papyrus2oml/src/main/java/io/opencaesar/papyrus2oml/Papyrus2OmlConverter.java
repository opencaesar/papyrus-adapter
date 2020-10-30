package io.opencaesar.papyrus2oml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;

public class Papyrus2OmlConverter {

	private static final String OML_EXTENSION = "oml";

	private File inputFolder;
	private File inputModelFile;
	private OmlCatalog catalog;
	private OmlWriter writer;
	private Logger logger;
	
	public Papyrus2OmlConverter(File inputFolder, File inputModelFile, OmlCatalog catalog, OmlWriter writer, Logger logger) {
		this.inputFolder = inputFolder;
		this.inputModelFile = inputModelFile;
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
		List<Resource> resources = new ArrayList<>();

		// derive the description bundle Name
		String fileName = rootModelFile.getName();
		String bundleName = fileName.substring(0, fileName.lastIndexOf('.'))+"-bundle";
		
		// derive the description bundle IRI
		String relativePath = inputFolder.toPath().normalize().relativize(rootModelFile.toPath()).toString();
		final String bundleIri = "http://"+relativePath.substring(0, relativePath.lastIndexOf('.'))+"-bundle";
		
		// create the description bundle
		final URI descriptionBundleUri = URI.createURI(catalog.resolveURI(bundleIri)+"."+OML_EXTENSION);
		logger.info("Creating: "+descriptionBundleUri);
		DescriptionBundle bundle = writer.createDescriptionBundle(descriptionBundleUri, bundleIri, SeparatorKind.HASH, bundleName);
		resources.add(bundle.eResource());

		//loop on all referenced UML models and convert them to descriptions
		{
			// convert the file to description
			Description description = createDescription(rootModelFile);
			
			// add description to the bundle
			writer.addDescriptionBundleInclusion(bundle, description.getIri(), null);
			
			// add the description resources
			resources.add(description.eResource());
		}
		
		return resources;
	}

	public Description createDescription(File modelFile) throws Exception {
		// derive the description bundle Name
		String fileName = modelFile.getName();
		String descriptionName = fileName.substring(0, fileName.lastIndexOf('.'));
		
		// derive the description bundle IRI
		String relativePath = inputFolder.toPath().normalize().relativize(modelFile.toPath()).toString();
		final String descriptionIri = "http://"+relativePath.substring(0, relativePath.lastIndexOf('.'));
		
		// create a description
		final URI descriptionUri = URI.createURI(catalog.resolveURI(descriptionIri)+"."+OML_EXTENSION);
		logger.info("Creating: "+descriptionUri);
		Description description = writer.createDescription(descriptionUri, descriptionIri, SeparatorKind.HASH, descriptionName);
		
		// populate the description based on model contents
		// ...
	
		return description;
	}
}
