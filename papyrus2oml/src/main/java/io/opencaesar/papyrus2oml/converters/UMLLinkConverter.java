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

import io.opencaesar.oml.Description;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Ontology;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class UMLLinkConverter extends LinkConverter {
	
	public UMLLinkConverter(Description description,String instanceIri, String relationIri, Object target , ConversionContext context ) {
		super(description,instanceIri,relationIri,target, context);
	}
	
	protected void createLink(Object value) {
		IdentifiedElement omlElement = getContext().umlToOml.get(value);
		if (omlElement==null) {
			return;
		}
		Ontology ont = omlElement.getOntology();
		String targetIri = omlElement.getIri();
		OMLUtil.addExtendsIfNeeded(getDescription(), ont.getIri(), getContext().builder);
		getContext().builder.addLinkAssertion(getDescription(), getInstanceIri(), getRelationIri(), targetIri);
	}

}
