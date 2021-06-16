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
package io.opencaesar.papyrus2oml.util;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.ElementImport;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageImport;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.converters.PackageConverter;
import io.opencaesar.papyrus2oml.converters.UMLNamedInstanceConverter;

public class UMLPackageConverter extends ResourceConverter {
	
	public UMLPackageConverter(Package rootPackage, List<String> ignoredIriPrefixes, OmlCatalog catalog, OmlBuilder builder, ResourceSet omlResourceSet, ConversionType conversionType, Logger logger) {
		super(new ConversionContext(ignoredIriPrefixes, catalog, builder, omlResourceSet,conversionType, logger));
		context.rootPackage = rootPackage;
		logger.info("UML converter in : " + (conversionType==ConversionType.uml? " UML mode" : "UML-DSL model"));
	}
	
	@Override
	public boolean shouldBeIgnored(EObject eObject) {
		if (eObject instanceof Package) {
			Package pkg = (Package) eObject;
			String iri = UmlUtils.getIRI(pkg);
			if (OMLUtil.shouldIgnoreIri(context.ignoredIriPrefixes, iri)) {
				return true;
			}
		}
		return !(eObject instanceof Element);
	}
	
	@Override
	public void finish() {
		context.deferredRelations.forEach(r -> r.run());
		context.deferredLinks.forEach(l -> l.run());
	}

	@Override
	public void convertEObject(EObject eObject) throws IOException {
		if (eObject == context.rootPackage) {
			PackageConverter.convertRootPackage(context.rootPackage, context);
		} else if (eObject instanceof Package) {
			PackageConverter.convertPackage((Package)eObject,context);
		} else if (eObject instanceof PackageImport || eObject instanceof ElementImport) {
			// this will be handled instead as an import statement added when an external element is referenced   
		} else if (eObject instanceof Element) {
			UMLNamedInstanceConverter.convert((Element)eObject,context);
		} else {
			context.logger.warn("Not Converted : " + eObject.eClass().getName());
		}
	}
}