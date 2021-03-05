package io.opencaesar.papyrus2oml.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
<<<<<<< HEAD
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Stereotype;

=======
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Stereotype;

import com.sun.source.doctree.StartElementTree;

>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
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
<<<<<<< HEAD
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class RelationConverter implements Runnable {
	private Element element;
=======

public class RelationConverter implements Runnable {
	private PackageableElement element;
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
	private ConversionContext context;
	private Description description;
	List<Member> types;
	List<Stereotype> stereotypes;

<<<<<<< HEAD
	public RelationConverter(Description description, Element element,
=======
	public RelationConverter(Description description, PackageableElement element,
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
			ConversionContext context, List<Member> types, List<Stereotype> stereotypes) {
		this.element = element;
		this.context = context;
		this.description = description;
		this.types = types;
		this.stereotypes = stereotypes;
	}

	@Override
	public void run() {
		RelationEntity umlOmlElement = (RelationEntity)context.getUmlOmlElementByName(element.eClass().getName());
		SourceRelation sourceR = umlOmlElement.getSourceRelation();
		List<String> sources = extractValues(element, context, description, sourceR);
		TargetRelation targetR = umlOmlElement.getTargetRelation();
		List<String> targets = extractValues(element, context, description, targetR);
<<<<<<< HEAD
		RelationInstance instance = context.writer.addRelationInstance(description,  UmlUtils.getName(element), sources,
=======
		RelationInstance instance = context.writer.addRelationInstance(description, element.getName(), sources,
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
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
	}

<<<<<<< HEAD
	private List<String> extractValues(Element element, ConversionContext context, Description description,
=======
	private List<String> extractValues(PackageableElement element, ConversionContext context, Description description,
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
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
