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

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class ConceptInstanceConverter {
	
	static public void convert(Element element, Description description, List<Stereotype> stereotypes, List<Member> types,
			ConversionContext context) {
		String instanceIri = UmlUtils.getIRI(description, element);
		Member instance = null;
		if (context.conversionType==ConversionType.dsl) {
			instance = context.builder.addConceptInstance(description,  UmlUtils.getName(element));
		}else if (!types.isEmpty() || !stereotypes.isEmpty()){
			instanceIri = UmlUtils.getUMLIRI(element, context);
			String ontIri = UmlUtils.getUMLONTIRI(element, context);
			OMLUtil.addExtendsIfNeeded(description, ontIri, context.builder);
			instance = OmlRead.getMemberByIri(description, instanceIri);
		}
		
		if (instance!=null) {
			context.umlToOml.put(element, instance);
			for (Member t : types) {
				context.builder.addConceptTypeAssertion(description, instanceIri, t.getIri());
			}
		
			// get the stereoType applications
			for (Stereotype stereoType : stereotypes) {
				EObject stApplication = element.getStereotypeApplication(stereoType);
				EClass eClass = stApplication.eClass();
				createAttributesAndReferences(description, context, instanceIri, stereoType, stApplication, eClass,false);
			}
		}
	}
	
	public static void createAttributes(Description description, ConversionContext context,
			String instanceIri, Stereotype stereoType, EObject stApplication, EClass eClass) {
		createAttributesAndReferences(description,context,instanceIri,stereoType,stApplication,eClass,true);
	}

	private static void createAttributesAndReferences(Description description, ConversionContext context,
			String instanceIri, Stereotype stereoType, EObject stApplication, EClass eClass, boolean attrOnly) {
		EList<Property> props = stereoType.allAttributes();
		for (Property prop : props) {
			if (context.shouldFilterFeature(prop)) {
				continue;
			}
			EStructuralFeature feature = eClass.getEStructuralFeature(prop.getName());
			if (feature==null) {
				continue;
			}
			Object val = stApplication.eGet(feature);
			if (prop.isMultivalued()) {
				EList<?> values = (EList<?>) val;
				if (!values.isEmpty()) {
					String propIRI = UmlUtils.getIri(prop);
					if (propIRI.isEmpty()) {
						context.logger.error("Could not get IRI for " + prop.getName());
						continue;
					}
					if (feature instanceof EAttribute) {
						for (Object value  : values) {
							if (prop.getType() instanceof Enumeration) {
								Enumeration _enum = (Enumeration) prop.getType();
								EnumerationLiteral literal = _enum.getOwnedLiteral(value.toString());
								value = UmlUtils.getOmlName(literal);
							}
							addScalarProperty(description, context, instanceIri, propIRI, value);
							// TODO: handle structure
						}
					} else if (!attrOnly) {
						addLink(description, context, instanceIri, val, propIRI);
					}
				}
			} else if (val!=null) {
				String propIRI = UmlUtils.getIri(prop);
				if (propIRI.isEmpty()) {
					context.logger.error("Could not get IRI for " + prop.getName());
					continue;
				}
				if (feature instanceof EAttribute) {
					addScalarProperty(description, context, instanceIri, propIRI, val);
				} else if (!attrOnly) {
					addLink(description, context, instanceIri, val, propIRI);
				}
			}
			
		}
	}

	private static void addLink(Description description, ConversionContext context, String instanceIri, Object val, String propIRI) {
		context.deferredLinks.add(new LinkConverter(description, instanceIri, propIRI, val, context ));
		String ontIRI = UmlUtils.getOntIRI(propIRI);
		OMLUtil.addUsesIfNeeded(description, ontIRI, context.builder);
	}

	private static void addScalarProperty(Description description, ConversionContext context, String instanceIri,
			String propIRI, Object value) {
		Literal literal = context.getLiteralValue(description, value);
		context.builder.addScalarPropertyValueAssertion(description, instanceIri, propIRI, literal);
	}
	
}
