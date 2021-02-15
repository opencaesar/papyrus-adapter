package io.opencaesar.papyrus2oml.converters;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Property;

import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class ConceptInstanceConverter {

	static public void convert(PackageableElement element, Description description, List<Member> types,
			ConversionContext context) {
		ConceptInstance instance = context.writer.addConceptInstance(description, element.getName());
		context.umlToOml.put(element, instance);
		String instanceIri = OmlRead.getIri(instance);
		for (Member t : types) {
			context.writer.addConceptTypeAssertion(description, instanceIri, OmlRead.getIri(t));
		}
		
		if (element instanceof Classifier) {
			Classifier clazz = (Classifier)element;
			EList<Property> props = clazz.getAttributes();
			for (Property prop : props) {
				System.out.println(prop.getLabel());
			}
		}
	}

}
