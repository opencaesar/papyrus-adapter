package io.opencaesar.papyrus2oml.converters;

import java.io.IOException;

<<<<<<< HEAD
import org.eclipse.uml2.uml.Element;
=======
import org.eclipse.uml2.uml.PackageableElement;
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965

import io.opencaesar.oml.Concept;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
<<<<<<< HEAD
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class UMLNamedInstanceConverter {
	
	static public void convert(Element element, ConversionContext context) throws IOException {
		String name = UmlUtils.getName(element);
		if (name != null && !name.isEmpty()) {
			if (name.equals("dronemodel")) {
				System.out.println("The one in questions");
			}
=======

public class UMLNamedInstanceConverter {
	
	static public void convert(PackageableElement element, ConversionContext context) throws IOException {
		String name = element.getName();
		if (name != null && !name.isEmpty()) {
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
			Description description = (Description) context.umlToOml.get(element.getNearestPackage());
			Member type = context.getUmlOmlElementByName(element.eClass().getName());
			if (type instanceof Concept) {
				UMLConceptInstanceConverter.convert(element, type, context);
			} else if (type instanceof RelationEntity) {
				context.deferred.add(new UMLRelationConverter(element,(RelationEntity) type, description, context));
			}
<<<<<<< HEAD
		}else {
			System.out.println("Did not convert");
=======
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
		}
	}
}
