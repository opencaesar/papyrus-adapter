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
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.papyrus2oml.util.ResourceConverter;

public class Ecore2OmlConverter {

	protected File inputModelFile;
	protected OmlCatalog catalog;
	protected OmlBuilder builder;
	protected Logger logger;
	
	public Ecore2OmlConverter(File inputModelFile, OmlCatalog catalog, OmlBuilder builder, Logger logger) {
		this.inputModelFile = inputModelFile;
		this.catalog = catalog;
		this.builder = builder;
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
		
		return builder.getNewResources();
		
	}
	
	protected ResourceSet createInputResourceSet() {
		return new ResourceSetImpl();
	}

	public Collection<ResourceConverter> getResourceConverters(Resource resource) throws IOException {
		return Collections.emptyList();
	}
}
