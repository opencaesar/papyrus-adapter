package io.opencaesar.papyrus2oml.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.PackageableElement;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.SourceRelation;
import io.opencaesar.oml.TargetRelation;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class UMLRelationConverter implements Runnable {

	PackageableElement element;
	private RelationEntity type;
	private ConversionContext context;
	private Description description;

	public UMLRelationConverter(PackageableElement element, RelationEntity type,Description description, ConversionContext context) {
		this.element = element;
		this.type = type;
		this.context = context;
		this.description = description;
	}

	@Override
	public void run() {
		RelationEntity entity = (RelationEntity) type;

		SourceRelation sourceR = entity.getSourceRelation();
		while (sourceR == null && !entity.getOwnedSpecializations().isEmpty()) {
			RelationEntity superEntity = entity.getOwnedSpecializations().stream().map(s -> s.getSpecializedTerm())
					.filter(t -> t instanceof RelationEntity).map(t -> (RelationEntity) t).findFirst().orElse(null);
			if (superEntity == null) {
				break;
			}
			sourceR = entity.getSourceRelation();
		}

		List<String> sources = new ArrayList<>();
		if (sourceR != null) {
			String name = sourceR.getName().split("_")[1];
			EStructuralFeature f = element.eClass().getEStructuralFeature(name);
			Object values = element.eGet(f);
			if (values instanceof Collection<?>) {
				for (Object value : ((Collection<?>) values)) {
					IdentifiedElement e = context.umlToOml.get(value);
					sources.add(OmlRead.getIri(e));
					OMLUtil.addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri(), context.writer);
				}
			} else {
				IdentifiedElement e = context.umlToOml.get(values);
				sources.add(OmlRead.getIri(e));
				OMLUtil.addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri(), context.writer);
			}
		}

		TargetRelation targetR = entity.getTargetRelation();
		while (targetR == null && !entity.getOwnedSpecializations().isEmpty()) {
			RelationEntity superEntity = entity.getOwnedSpecializations().stream().map(s -> s.getSpecializedTerm())
					.filter(t -> t instanceof RelationEntity).map(t -> (RelationEntity) t).findFirst().orElse(null);
			if (superEntity == null) {
				break;
			}
			targetR = entity.getTargetRelation();
		}

		List<String> targets = new ArrayList<>();
		if (targetR != null) {
			String name = targetR.getName().split("_")[1];
			EStructuralFeature f = element.eClass().getEStructuralFeature(name);
			Object values = element.eGet(f);
			if (values instanceof Collection<?>) {
				for (Object value : ((Collection<?>) values)) {
					IdentifiedElement e = context.umlToOml.get(value);
					targets.add(OmlRead.getIri(e));
					OMLUtil.addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri(), context.writer);
				}
			} else {
				IdentifiedElement e = context.umlToOml.get(values);
				targets.add(OmlRead.getIri(context.umlToOml.get(values)));
				OMLUtil.addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri(), context.writer);
			}
		}

		RelationInstance instance = context.writer.addRelationInstance(description, element.getName(), sources,
				targets);
		context.writer.addRelationTypeAssertion(description, OmlRead.getIri(instance), OmlRead.getIri(type));
		UMLConceptInstanceConverter.createAttributes(element, context, description, OmlRead.getIri(instance));
	}
}
