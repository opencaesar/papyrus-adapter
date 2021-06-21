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
package io.opencaesar.papyrus2oml.converters;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.uml2.uml.Package;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class PackageConverter {

	static public void convertPackage(Package package_, ConversionContext context) throws IOException {
		convertPackage(package_, "", context);
	}
	
	static public void convertPackage(Package package_, String postFix, ConversionContext context) throws IOException {
		boolean empty = package_.getPackagedElements().stream().filter(e -> !(e instanceof Package)).count() == 0;

		if (!empty) {
			final String prefix = package_.getName();
			String iri = UmlUtils.getIRI(package_);
			String calcuatedPostFix = postFix.isEmpty() ? "" : ("-" + postFix);
			iri += calcuatedPostFix;
			final URI uri = URI.createURI(context.catalog.resolveURI(iri) + "." + OmlConstants.OML_EXTENSION);
			Description description = context.builder.createDescription(uri, iri, SeparatorKind.HASH, prefix+calcuatedPostFix);
			context.umlToOml.put(package_, description);

			context.builder.addDescriptionUsage(description, OmlConstants.OWL_IRI, null);
			if(context.descriptionBundle!=null) {
				context.builder.addDescriptionBundleInclusion(context.descriptionBundle, iri, null);
			}
		}
	}

	static public void convertRootPackage(Package package_, ConversionContext context) throws IOException {
		convertRootPackage(package_, "", context);
	}
	
	static public void convertRootPackage(Package package_,String postFix, ConversionContext context) throws IOException {
		final String prefix = package_.getName();
		String iri = UmlUtils.getIRI(package_);
		boolean empty = package_.getPackagedElements().stream().filter(e -> !(e instanceof Package)).count() == 0;
		String calcuatedPostFix = (postFix.isEmpty() || context.conversionType == ConversionType.uml_dsl) ? "" : ("-" + postFix);
		iri = empty ? iri + calcuatedPostFix : iri + "-bundle";
		final URI uri = URI.createURI(context.catalog.resolveURI(iri) +   "." + OmlConstants.OML_EXTENSION);
		if (context.conversionType != ConversionType.uml_dsl || context.DSL) {
			DescriptionBundle bundle = context.builder.createDescriptionBundle(uri, iri, SeparatorKind.HASH, prefix + calcuatedPostFix);
			context.descriptionBundle = bundle;
			if (context.conversionType == ConversionType.uml) {
				context.builder.addDescriptionBundleUsage(bundle, UmlUtils.UML_BUNDLE_IRI, null);
			}
		}
		if (!empty) {
			convertPackage(package_, postFix, context);
		}
	}
}
