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
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.ProfileApplication;

import io.opencaesar.oml.Import;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.VocabularyBundle;
import io.opencaesar.oml.VocabularyBundleExtension;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class ProfileApplicationConverter {

	static public void convert(ProfileApplication pa, ConversionContext context) throws IOException {
		Profile profile = pa.getAppliedProfile();
		String iri = UmlUtils.getIRI(profile);
		if (iri != null && !iri.isEmpty()) {
			if (context.conversionType == ConversionType.uml_dsl) {
				context.builder.addDescriptionBundleUsage(context.descriptionBundle, iri, null);
			} else {
				Resource resource = context.descriptionBundle.eResource();
				URI uri = OmlRead.getResolvedUri(resource, URI.createURI(iri));
				if (uri != null) {
					Resource ontologyResource = resource.getResourceSet().getResource(uri, true);
					if (ontologyResource != null) {
						Ontology ontology = OmlRead.getOntology(ontologyResource);
						if (ontology instanceof VocabularyBundle) {
							VocabularyBundle bundle = (VocabularyBundle) ontology;
							for (Import i : bundle.getOwnedImports()) {
								if (i instanceof VocabularyBundleExtension) {
									if (!i.getUri().equals(UmlUtils.UML_BUNDLE_IRI)) {
										context.builder.addDescriptionBundleUsage(context.descriptionBundle, i.getUri(), null);
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
