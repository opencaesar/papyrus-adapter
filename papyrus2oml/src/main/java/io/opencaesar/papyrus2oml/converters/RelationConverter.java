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
		System.out.print(".");
		String instanceIri= "";
		Member instance = null;
		if (context.conversionType!=ConversionType.uml_dsl) {
			List<String> sources = null;
			List<String> targets = null;
			if (element instanceof Association) {
				sources = getAssociationEnds((Association)element, context, description, true);
				targets = getAssociationEnds((Association)element, context, description, false);
			}else {
				String sourceName = getFeatureName(element, true, context);
				String targetName = getFeatureName(element, false, context);
				sources = extractValues(element, context, description, sourceName);
				targets = extractValues(element, context, description, targetName);
			}
			instance = context.writer.addRelationInstance(description, UmlUtils.getName(element), sources,targets);
			instanceIri = OmlRead.getIri(instance);
		}else if (!types.isEmpty()){
			instanceIri = UmlUtils.getUMLIRI(element, context);
			String ontIri = UmlUtils.getUMLONTIRI(element, context);
			OMLUtil.addExtendsIfNeeded(description, ontIri, context.writer);
			instance = OmlRead.getMemberByIri(description, instanceIri);
		}
		
		if (instance==null || instanceIri.isEmpty()) {
			System.out.println("error");
		}

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
		OMLUtil.addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri(), context.writer);
		return OmlRead.getIri(e);
	}

	private static String getFeatureName(Element element, boolean source, ConversionContext context) {
		Member umlOmlElement = context.getUmlOmlElementByName(element.eClass().getName());
		Member namedMember = null;
		if (source) {
			namedMember = ((RelationEntity) umlOmlElement).getSourceRelation();
		} else {
			namedMember = ((RelationEntity) umlOmlElement).getTargetRelation();
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
			result.add(OmlRead.getIri(e));
			OMLUtil.addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri(), context.writer);
		}
		return result;
	}
}
