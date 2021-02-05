package io.opencaesar.papyrus2oml;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;
import io.opencaesar.papyrus2oml.util.ResourceConverter;

public class Ecore2OmlConverter {

	protected File inputModelFile;
	protected OmlCatalog catalog;
	protected OmlWriter writer;
	protected Logger logger;
	
	public Ecore2OmlConverter(File inputModelFile, OmlCatalog catalog, OmlWriter writer, Logger logger) {
		this.inputModelFile = inputModelFile;
		this.catalog = catalog;
		this.writer = writer;
		this.logger = logger;
	}

	public Collection<Resource> convert() throws IOException {
		final String path = inputModelFile.getCanonicalPath();
	
		// create input resource set
		ResourceSet resourceSet = createInputResourceSet();

		// load input model file
		Resource resource = resourceSet.getResource(URI.createFileURI(path), true);
		
		// convert resource
		Collection<ResourceConverter> converters = getResourceConverters(resource);
		
		// invoke each converter
		for (ResourceConverter converter : converters) {
			
			// get object iterator
			TreeIterator<EObject> i = resource.getAllContents();
			
			// iterate through the objects to convert them
			while (i.hasNext()) {
				EObject eObject = i.next();
				if (converter.shouldBeIgnored(eObject)) {
					i.prune();
				} else {
					converter.convertEObject(eObject);
				}
			}
			
			// finish conversion
			converter.finish();
		}
		
		return writer.getNewResources();
		
	}
	
	protected ResourceSet createInputResourceSet() {
		return new ResourceSetImpl();
	}

	public Collection<ResourceConverter> getResourceConverters(Resource resource) throws IOException {
		return Collections.emptyList();
	}
}
