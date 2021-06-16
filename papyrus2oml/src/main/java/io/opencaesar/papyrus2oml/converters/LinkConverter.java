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

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.util.UMLUtil;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Ontology;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class LinkConverter implements Runnable {
	private ConversionContext context;
	private Description description;
	private String instanceIri;
	private String relationIri;
	private Object target;
	
	public LinkConverter(Description description,String instanceIri, String relationIri, Object target , ConversionContext context ) {
		this.context = context;
		this.description = description;
		this.instanceIri = instanceIri;
		this.relationIri = relationIri;
		this.target = target;
	}
	
	public Description getDescription() {
		return description;
	}
	
	public String getInstanceIri() {
		return instanceIri;
	}
	
	public ConversionContext getContext() {
		return context;
	}
	
	public String getRelationIri() {
		return relationIri;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void run() {
		if (target instanceof List) {
			List elements = (List)target;
			for (Object value : elements) {
				createLink(value);
			}
		}else {
			createLink(target);
		}
	}

	protected void createLink(Object value) {
		EObject eValue = (EObject)value;		
		Element baseElemnt = UMLUtil.getBaseElement(eValue);
		IdentifiedElement omlElement = context.umlToOml.get(baseElemnt);
		if (omlElement==null) {
			omlElement = context.getOmlElementForIgnoredElement(baseElemnt, description) ;
		}
		Ontology ont = omlElement.getOntology();
		String targetIri = omlElement.getIri();
		OMLUtil.addExtendsIfNeeded(description, ont.getIri(), context.builder);
		context.builder.addLinkAssertion(description, instanceIri, relationIri, targetIri);
	}
	
}
