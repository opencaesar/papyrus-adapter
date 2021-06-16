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
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Stereotype;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Import;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class UMLNamedInstanceConverter {
	private static final String CONCEPT_POSTFIX = "_Concept";
	private static final String RELATION_POSTFIX = "_Relation";
	
	static public void convert(Element element, ConversionContext context) throws IOException {
		String name = UmlUtils.getName(element);
		if (name != null && !name.isEmpty()) {
			Description description = (Description) context.umlToOml.get(element.getNearestPackage());
			if (description == null) {
				return;// parent package was not mapped
			}
			ResourceSet rs = description.eResource().getResourceSet();
			Member type = context.getUmlOmlElementByName(element.eClass().getName());
			if (type instanceof Concept) {
				UMLConceptInstanceConverter.convert(element, type, context);
			} else if (type instanceof RelationEntity) {
				context.deferredRelations.add(new UMLRelationConverter(element,(RelationEntity) type, description, context));
			} else if (type instanceof Aspect){
				/// UML_DSL and stereotyped => relation
				if (context.conversionType == ConversionType.uml_dsl && shouldCreateRelation(element, description,rs, context)) {
					createRelationInstance(description, element, context);
				} else {
					createConceptInstance(element, context);
				}
			}else {
				context.logger.warn("Did not convert: " + element);
			}
		}else {
			context.logger.warn("Did not convert: " + element);
		}
	}

	private static void createConceptInstance(Element element, ConversionContext context) {
		Concept conceptType = (Concept) context.getUmlOmlElementByName(element.eClass().getName() + CONCEPT_POSTFIX);
		UMLConceptInstanceConverter.convert(element, conceptType, context);
	}

	private static void createRelationInstance(Description description, Element element, ConversionContext context) {
		RelationEntity relType = (RelationEntity) context.getUmlOmlElementByName(element.eClass().getName() + RELATION_POSTFIX);
		context.deferredRelations.add(new UMLRelationConverter(element, relType, description, context));
	}

	private static boolean shouldCreateRelation(Element element, Description description, ResourceSet rs, ConversionContext context) {
		// check if there is an stereotype for association 
		List<Stereotype> stereotypes = element.getAppliedStereotypes();
		for (Stereotype sterotype : stereotypes) {
			Package package_ = sterotype.getNearestPackage();
			Import i = OMLUtil.addUsesIfNeeded(description, UmlUtils.getIRI(package_), context.builder);
			Member type = (Member) context.umlToOml.get(sterotype);
			if (type == null) {
				URI uri = OmlRead.getResolvedUri(i);
				if (uri == null) {
					throw new RuntimeException("Cannot resolve IRI '" + UmlUtils.getIRI(package_) + "'");
				}
				Resource r = rs.getResource(uri, true);
				Vocabulary vocabulary = (Vocabulary) OmlRead.getOntology(r);
				if (vocabulary == null) {
					throw new RuntimeException("Cannot load vocabylary '" + UmlUtils.getIRI(package_) + "'");
				}
				type = OmlRead.getMemberByName(vocabulary, sterotype.getName());
				if (type == null) {
					throw new RuntimeException("Cannot find entity equivalent to '" + sterotype.getQualifiedName() + "'");
				}
				if (type instanceof RelationEntity) {
					return true;
				}
			}
		}
		return false;
	}
}
