package io.opencaesar.papyrus2oml.converters;

import java.io.IOException;
import java.util.Collections;
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
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class UMLNamedInstanceConverter {
	private static final String CONCEPT_POSTFIX = "_concept";
	private static final String RELATION_POSTFIX = "_relation";
	
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
				context.deferred.add(new UMLRelationConverter(element,(RelationEntity) type, description, context));
			} else if (type instanceof Aspect){
				/// UML_DSL and stereo typed => relation
				if (context.conversionType == ConversionType.uml_dsl && 
					shouldCreateRelation(element, description,rs, context)) {
					Member relType = context.getUmlOmlElementByName(element.eClass().getName() + RELATION_POSTFIX);
					Ontology ont = OmlRead.getOntology(relType);
					RelationInstance instance = context.writer.addRelationInstance(description,  UmlUtils.getName(element), Collections.emptyList(),Collections.emptyList());
					context.writer.addRelationTypeAssertion(description,OmlRead.getIri(instance), OmlRead.getIri(relType));
					OMLUtil.addExtendsIfNeeded(description, ont.getIri(), context.writer);
				} else {
					createCooncept(element, context);
				}
			}else {
				context.logger.warn("Did not convert: " + element);
			}
		}else {
			context.logger.warn("Did not convert: " + element);
		}
	}

	private static void createCooncept(Element element, ConversionContext context) {
		Member conceptType = context.getUmlOmlElementByName(element.eClass().getName() + CONCEPT_POSTFIX);
		UMLConceptInstanceConverter.convert(element, conceptType, context);
	}

	private static boolean shouldCreateRelation(Element element, Description description, ResourceSet rs, ConversionContext context) {
		// check if there is an stereotype for association 
		List<Stereotype> stereotypes = element.getAppliedStereotypes();
		for (Stereotype sterotype : stereotypes) {
			Package package_ = sterotype.getNearestPackage();
			Import i = OMLUtil.addUsesIfNeeded(description, UmlUtils.getIRI(package_), context.writer);
			Member type = (Member) context.umlToOml.get(sterotype);
			if (type == null) {
				URI uri = OmlRead.getResolvedImportUri(i);
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
