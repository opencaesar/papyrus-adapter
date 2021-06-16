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

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.Element;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class UMLConceptInstanceConverter {
	
	public static void convert(Element element, Member type,ConversionContext context ) {
		// attributes in this case are the properties of the element.eClass
		Description description = (Description) context.umlToOml.get(element.getNearestPackage());
		ConceptInstance instance = context.builder.addConceptInstance(description, UmlUtils.getName(element));
		String instanceIRI = instance.getIri();
		context.builder.addConceptTypeAssertion(description, instanceIRI, type.getIri());
		context.umlToOml.put(element, instance);
		OMLUtil.addUsesIfNeeded(description,  type.getOntology().getIri(), context.builder);	
		createAttributes(element, context, description, instanceIRI);
		createReferences(element, context, description, instanceIRI);
	}

	public static void createAttributes(Element element, ConversionContext context, Description description,
			String instanceIRI) {
		EClass umlclass = element.eClass();
		EList<EAttribute> attrs = umlclass.getEAllAttributes();
		for (EAttribute feature : attrs) {
			if (context.shouldFilterFeature(feature)) {
				continue;
			}
			Object val = element.eGet(feature);
			if (feature.isMany()) {
				EList<?> values = (EList<?>) val;
				if (!values.isEmpty()) {
					String propIRI = getIri(feature);
					for (Object value  : values) {
						createProperty(context, description, instanceIRI, propIRI, value);
						// TODO: handle structure
					}
				}
			}else if (val!=null) {
				String propIRI = getIri(feature);
				createProperty(context, description, instanceIRI, propIRI, val);
			}
		}
	}
	
	public static void createReferences(Element element, ConversionContext context, Description description,
			String instanceIRI) {
		EClass umlclass = element.eClass();
		EList<EReference> attrs = umlclass.getEAllReferences();
		for (EReference feature : attrs) {
			if (context.shouldFilterFeature(feature)) {
				continue;
			}
			Object val = element.eGet(feature);
			if (feature.isMany()) {
				EList<?> values = (EList<?>) val;
				if (!values.isEmpty()) {
					String propIRI = getIri(feature);
					context.deferredLinks.add(new UMLLinkConverter(description, instanceIRI, propIRI, val, context ));
				}
			}else if (val!=null) {
				String propIRI = getIri(feature);
				context.deferredLinks.add(new UMLLinkConverter(description, instanceIRI, propIRI, val, context ));
			}
		}
	}

	private static void createProperty(ConversionContext context, Description description, String instanceIRI,
			String propIRI, Object value) {
		Literal literal = context.getLiteralValue(description, value);
		context.builder.addScalarPropertyValueAssertion(description,  instanceIRI, propIRI, literal);
	}

	private static String getIri(EStructuralFeature feature) {
		return UmlUtils.UML_NS + StringExtensions.toFirstLower(feature.getContainerClass().getSimpleName()) + "_" + feature.getName();
	}

}
