package io.opencaesar.papyrus2oml.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.PackageableElement;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.SourceRelation;
import io.opencaesar.oml.TargetRelation;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class RelationConverter implements Runnable {
	private PackageableElement element;
	private ConversionContext context;
	private Description description;
	List<Member> types;

	public RelationConverter(Description description, PackageableElement element,
			ConversionContext context, List<Member> types) {
		this.element = element;
		this.context = context;
		this.description = description;
		this.types = types;
	}

	@Override
	public void run() {
		RelationEntity umlOmlElement = (RelationEntity)context.getUmlOmlElementByName(element.eClass().getName());
		SourceRelation sourceR = umlOmlElement.getSourceRelation();
		List<String> sources = extractValues(element, context, description, sourceR);
		TargetRelation targetR = umlOmlElement.getTargetRelation();
		List<String> targets = extractValues(element, context, description, targetR);
		RelationInstance instance = context.writer.addRelationInstance(description, element.getName(), sources,
				targets);
		String instanceIri = OmlRead.getIri(instance);
		for (Member t : types) {
			context.writer.addRelationTypeAssertion(description, instanceIri, OmlRead.getIri(t));
		}
	}

	private List<String> extractValues(PackageableElement element, ConversionContext context, Description description,
			Relation relation) {
		List<String> result = new ArrayList<>();
		if (relation != null) {
			String name = relation.getName().split("_")[1];
			EStructuralFeature f = element.eClass().getEStructuralFeature(name);
			Object values = element.eGet(f);
			Collection<?> valueAsCollection = null;
			if (values instanceof Collection<?>) {
				valueAsCollection = (Collection<?>) values;
			} else {
				valueAsCollection = Collections.singleton(values);
			}
			for (Object value : valueAsCollection) {
				IdentifiedElement e = context.umlToOml.get(value);
				result.add(OmlRead.getIri(e));
				OMLUtil.addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri(), context.writer);
			}
		}
		return result;
	}
}
