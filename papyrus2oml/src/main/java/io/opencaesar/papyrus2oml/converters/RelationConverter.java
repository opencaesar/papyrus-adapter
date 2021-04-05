package io.opencaesar.papyrus2oml.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.util.OmlRead;
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
		System.out.print(".");
		String sourceName = getFeatureName(element, true, context);
		String targetName = getFeatureName(element, false, context);
		List<String> sources = extractValues(element, context, description, sourceName);
		List<String> targets = extractValues(element, context, description, targetName);
		RelationInstance instance = context.writer.addRelationInstance(description, UmlUtils.getName(element), sources,
				targets);
		String instanceIri = OmlRead.getIri(instance);
		int index = 0;
		for (Member t : types) {
			context.writer.addRelationTypeAssertion(description, instanceIri, OmlRead.getIri(t));
			Stereotype st = stereotypes.get(index);
			EObject stApp = element.getStereotypeApplication(st);
			EClass eClass = stApp.eClass();
			ConceptInstanceConverter.createAttributes(description, context, instanceIri, st, stApp, eClass);
			index++;
		}
		context.umlToOml.put(element, instance);
	}

	private static String getFeatureName(Element element, boolean source, ConversionContext context) {
		Member umlOmlElement = context.getUmlOmlElementByName(element.eClass().getName());
		Member namedMember;
		if (source) {
			if (umlOmlElement instanceof RelationEntity) {
				namedMember = ((RelationEntity) umlOmlElement).getSourceRelation();
			} else {
				// this means it is an association
				return "ownedEnd";
			}
		} else {
			if (umlOmlElement instanceof RelationEntity) {
				namedMember = ((RelationEntity) umlOmlElement).getTargetRelation();
			} else {
				return "navigableOwnedEnd";
			}
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
			if (value instanceof Property) {
				value = ((Property) value).getType();
			}
			IdentifiedElement e = context.umlToOml.get(value);
			if (e == null) {
				System.out.println("problem");
			} else {
				result.add(OmlRead.getIri(e));
				OMLUtil.addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri(), context.writer);
			}
		}
		return result;
	}
}
