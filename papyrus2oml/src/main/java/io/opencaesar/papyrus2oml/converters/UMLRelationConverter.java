package io.opencaesar.papyrus2oml.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.Element;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.SourceRelation;
import io.opencaesar.oml.TargetRelation;
import io.opencaesar.oml.util.OmlRead;
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
	
	private static RelationInstance createInstance(RelationEntity type,Element element,ConversionContext context, Description description) {
		RelationEntity entity = (RelationEntity) type;
		SourceRelation sourceR = entity.getSourceRelation();
		while (sourceR == null && !entity.getOwnedSpecializations().isEmpty()) {
			RelationEntity superEntity = entity.getOwnedSpecializations().stream().map(s -> s.getSpecializedTerm())
					.filter(t -> t instanceof RelationEntity).map(t -> (RelationEntity) t).findFirst().orElse(null);
			if (superEntity == null) {
				break;
			}
			entity = superEntity;
			sourceR = entity.getSourceRelation();
		}

		List<String> sources = convertElements(element, context, description, sourceR);

		TargetRelation targetR = entity.getTargetRelation();
		while (targetR == null && !entity.getOwnedSpecializations().isEmpty()) {
			RelationEntity superEntity = entity.getOwnedSpecializations().stream().map(s -> s.getSpecializedTerm())
					.filter(t -> t instanceof RelationEntity).map(t -> (RelationEntity) t).findFirst().orElse(null);
			if (superEntity == null) {
				break;
			}
			entity = superEntity;
			targetR = entity.getTargetRelation();
		}
		
		List<String> targets = convertElements(element, context, description, targetR);
		RelationInstance instance = context.writer.addRelationInstance(description,  UmlUtils.getName(element), sources,
				targets);
		context.writer.addRelationTypeAssertion(description, OmlRead.getIri(instance), OmlRead.getIri(type));
		UMLConceptInstanceConverter.createAttributes(element, context, description, OmlRead.getIri(instance));
		context.umlToOml.put(element, instance);
		return instance;
	}

	private static List<String> convertElements(Element element, ConversionContext context, Description description,
			Member sourceR) {
		List<String> elements = new ArrayList<>();
		if (sourceR != null) {
			String name = sourceR.getName().split("_")[1];
			EStructuralFeature f = element.eClass().getEStructuralFeature(name);
			Object values = element.eGet(f);
			if (values instanceof Collection<?>) {
				for (Object value : ((Collection<?>) values)) {
					IdentifiedElement e = context.umlToOml.get(value);
					if (e==null) {
						// to avoid order dependency
						Element sourceELment = (Element)value;
						Member srcType = context.getUmlOmlElementByName(sourceELment.eClass().getName());
						createInstance((RelationEntity)srcType, sourceELment, context, description);
						e = context.umlToOml.get(value);
					}
					elements.add(OmlRead.getIri(e));
					Ontology ont = OmlRead.getOntology(e);
					OMLUtil.addExtendsIfNeeded(description, ont.getIri(), context.writer);
				}
			} else {
				IdentifiedElement e = context.umlToOml.get(values);
				elements.add(OmlRead.getIri(e));
				OMLUtil.addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri(), context.writer);
			}
		}
		return elements;
	}
}
