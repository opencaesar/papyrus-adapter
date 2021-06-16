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
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.ProfileApplication;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.converters.NamedInstanceConverter;
import io.opencaesar.papyrus2oml.converters.PackageConverter;
import io.opencaesar.papyrus2oml.converters.ProfileApplicationConverter;

public class DSLPackageConverter extends ResourceConverter {
	
	public DSLPackageConverter(Package rootPackage, Profile profile, List<String> ignoredIriPrefixes, OmlCatalog catalog,
			OmlBuilder builder, ResourceSet rs, ConversionType conversionType, Logger logger) {
		super(new ConversionContext(ignoredIriPrefixes, catalog, builder, rs,conversionType, logger));
		context.rootPackage = rootPackage;
		context.DSL =true;
		logger.info("DLS converter in : " + (conversionType==ConversionType.dsl? " DSL mode" : "UML-DSL model"));
		if (conversionType==ConversionType.uml_dsl) {
			context.postFix = profile.getName();
		}
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
		context.logger.info("Reations Conversion: ");
		context.deferredRelations.forEach(r -> r.run());
		context.deferredLinks.forEach(l -> l.run());
	}

	@Override
	public void convertEObject(EObject eObject) throws IOException {
		if (eObject == context.rootPackage) {
			PackageConverter.convertRootPackage(context.rootPackage,context.postFix, context);
		} else if (eObject instanceof Package) {
			PackageConverter.convertPackage((Package) eObject,context.postFix, context);
		} else if (eObject instanceof ProfileApplication) {
			ProfileApplicationConverter.convert((ProfileApplication) eObject, context);
		} else if (eObject instanceof Element) {
			NamedInstanceConverter.convert((Element) eObject, context);
		} else {
			context.logger.warn("Not Converted : " + eObject.eClass().getName());
		}
	}
}