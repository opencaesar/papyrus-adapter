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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class RelationConverter implements Runnable {
	private Element element;
	private ConversionContext context;
	private Description description;
	List<Member> types;
	List<Stereotype> stereotypes;

	public RelationConverter(Description description, Element element, ConversionContext context, List<Member> types,
			List<Stereotype> stereotypes) {
		this.element = element;
		this.context = context;
		this.description = description;
		this.types = types;
		this.stereotypes = stereotypes;
	}

	@Override
	public void run() {
		String instanceIri= "";
		Member instance = null;
		if (context.conversionType!=ConversionType.uml_dsl) {
			List<String> sources = null;
			List<String> targets = null;
			if (element instanceof Association) {
				sources = getAssociationEnds((Association)element, context, description, true);
				targets = getAssociationEnds((Association)element, context, description, false);
			} else {
				String sourceName = getFeatureName(element, true, context);
				String targetName = getFeatureName(element, false, context);
				sources = extractValues(element, context, description, sourceName);
				targets = extractValues(element, context, description, targetName);
			}
			instance = context.builder.addRelationInstance(description, UmlUtils.getName(element), sources, targets);
			instanceIri = instance.getIri();
		}else if (!types.isEmpty()){
			instanceIri = UmlUtils.getUMLIRI(element, context);
			String ontIri = UmlUtils.getUMLONTIRI(element, context);
			OMLUtil.addExtendsIfNeeded(description, ontIri, context.builder);
			instance = OmlRead.getMemberByIri(description, instanceIri);
		}
		
		assert (instance!=null && !instanceIri.isEmpty());

		int index = 0;
		for (Member t : types) {
			context.builder.addRelationTypeAssertion(description, instanceIri, t.getIri());
			Stereotype st = stereotypes.get(index);
			EObject stApp = element.getStereotypeApplication(st);
			EClass eClass = stApp.eClass();
			ConceptInstanceConverter.createAttributes(description, context, instanceIri, st, stApp, eClass);
			index++;
		}
		context.umlToOml.put(element, instance);
	}

	private List<String> getAssociationEnds(Association association, ConversionContext context2, Description description2,
			boolean source) {
		EList<Property> ends = association.getMemberEnds();
		if (source) {
			List<String> result = new ArrayList<>();
			for (int index = 0 ; index < ends.size()-1; index++) {
				result.add(getIRI(ends.get(index)));
			}
			return result;
		}
		return Collections.singletonList(getIRI(ends.get(ends.size()-1)));
	}

	private String getIRI(Property property) {
		Type value = property.getType();
		IdentifiedElement e = context.umlToOml.get(value);
		OMLUtil.addExtendsIfNeeded(description, e.getOntology().getIri(), context.builder);
		return e.getIri();
	}

	private static String getFeatureName(Element element, boolean source, ConversionContext context) {
		RelationEntity entity = (RelationEntity) context.getUmlOmlElementByName(element.eClass().getName());
		Member namedMember = null;
		if (source) {
			namedMember = OMLUtil.getSourceRelation(entity, context);
		} else {
			namedMember = OMLUtil.getTargetRelation(entity, context);
		}
		return namedMember.getName().split("_")[1];
	}

	private List<String> extractValues(Element element, ConversionContext context, Description description,
			String featureName) {
		List<String> result = new ArrayList<>();
		EStructuralFeature f = element.eClass().getEStructuralFeature(featureName);
		Object values = element.eGet(f);
		Collection<?> valueAsCollection = null;
		if (values instanceof Collection<?>) {
			valueAsCollection = (Collection<?>) values;
		} else {
			valueAsCollection = Collections.singleton(values);
		}
		for (Object value : valueAsCollection) {
			IdentifiedElement e = context.umlToOml.get(value);
			if (e==null) {
				e = context.getOmlElementForIgnoredElement((Element)value, description) ;
			}
			result.add(e.getIri());
			OMLUtil.addExtendsIfNeeded(description, e.getOntology().getIri(), context.builder);
		}
		return result;
	}
}
