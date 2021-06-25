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
import java.util.List;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Type;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class UMLRelationConverter implements Runnable {

	Element element;
	private RelationEntity type;
	private ConversionContext context;
	private Description description;

	public UMLRelationConverter(Element element, RelationEntity type,Description description, ConversionContext context) {
		this.element = element;
		this.type = type;
		this.context = context;
		this.description = description;
	}

	@Override
	public void run() {
		createInstance((RelationEntity) type,element,context,description);
	}
	
	private static RelationInstance createInstance(RelationEntity type, Element element, ConversionContext context, Description description) {
		try {
			RelationEntity entity = (RelationEntity) type;
			List<String> sources = new ArrayList<>();
			if (element instanceof Association) {
				Association assoc = (Association) element;
				Type source = assoc.getMemberEnds().get(0).getType();
				IdentifiedElement e = context.umlToOml.get(source);
				sources.add(e.getIri());
			} else {
				Relation sourceR = OMLUtil.getSourceRelation(entity, context);
				while (sourceR == null && !entity.getOwnedSpecializations().isEmpty()) {
					RelationEntity superEntity = entity.getOwnedSpecializations().stream()
							.map(s -> s.getSpecializedTerm())
							.filter(t -> t instanceof RelationEntity)
							.map(t -> (RelationEntity) t)
							.findFirst().orElse(null);
					if (superEntity == null) {
						break;
					}
					entity = superEntity;
					sourceR = OMLUtil.getSourceRelation(entity, context);
				}
				sources.addAll(convertElements(element, context, description, sourceR));
			}
			List<String> targets = new ArrayList<>();
			if (element instanceof Association) {
				Association assoc = (Association) element;
				Type target = assoc.getMemberEnds().get(1).getType();
				IdentifiedElement e = context.umlToOml.get(target);
				targets.add(e.getIri());
			} else {
				Relation targetR = OMLUtil.getTargetRelation(entity, context);
				while (targetR == null && !entity.getOwnedSpecializations().isEmpty()) {
					RelationEntity superEntity = entity.getOwnedSpecializations().stream()
							.map(s -> s.getSpecializedTerm())
							.filter(t -> t instanceof RelationEntity)
							.map(t -> (RelationEntity) t)
							.findFirst().orElse(null);
					if (superEntity == null) {
						break;
					}
					entity = superEntity;
					targetR = OMLUtil.getTargetRelation(entity, context);
				}
				targets = convertElements(element, context, description, targetR);
			}
			RelationInstance instance = context.builder.addRelationInstance(description,  UmlUtils.getName(element), sources, targets);
			context.builder.addRelationTypeAssertion(description, instance.getIri(), type.getIri());
			UMLConceptInstanceConverter.createAttributes(element, context, description, instance.getIri());
			UMLConceptInstanceConverter.createReferences(element, context, description, instance.getIri());
			context.umlToOml.put(element, instance);
			return instance;
		} catch (UnsupportedOperationException exp) {
			context.logger.warn(exp.getMessage());
			return null;
		}
	}

	private static List<String> convertElements(Element element, ConversionContext context, Description description, Relation relation) {
		List<String> elements = new ArrayList<>();
		if (relation != null) {
			String name = relation.getName().split("_")[1];
			EStructuralFeature f = element.eClass().getEStructuralFeature(name);
			Object values = element.eGet(f);
			if (values instanceof Collection<?>) {
				for (Object value : ((Collection<?>) values)) {
					if (value instanceof Package) {
						throw new UnsupportedOperationException("Can not handle relations with package end yet");
					}
					IdentifiedElement e = context.umlToOml.get(value);
					if (e==null) {
						e = context.getOmlElementForIgnoredElement((Element)value, description) ;
						if (e==null) {
							// should happen only if the element is a relation 
							// just in case we have a relation with source relation
							NamedElement sourceELment = (NamedElement)value;
							Member srcType = context.getUmlOmlElementByName(sourceELment.eClass().getName());
							createInstance((RelationEntity)srcType, sourceELment, context, description);
							e = context.umlToOml.get(value);
						}
					}
					elements.add(e.getIri());
					Ontology ont = e.getOntology();
					OMLUtil.addExtendsIfNeeded(description, ont.getIri(), context.builder);
				}
			} else {
				if (values instanceof Package) {
					throw new UnsupportedOperationException("Can not handle relations with package end yet");
				}
				IdentifiedElement e = context.umlToOml.get(values);
				if (e==null) {
					e = context.getOmlElementForIgnoredElement((Element)values, description) ;
				}
				elements.add(e.getIri());
				OMLUtil.addExtendsIfNeeded(description, e.getOntology().getIri(), context.builder);
			}
		}
		return elements;
	}
}
