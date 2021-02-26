package io.opencaesar.papyrus2oml.converters;

import java.io.IOException;

import org.eclipse.uml2.uml.PackageableElement;

import io.opencaesar.oml.Concept;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class UMLNamedInstanceConverter {
	
	static public void convert(PackageableElement element, ConversionContext context) throws IOException {
		String name = element.getName();
		if (name != null && !name.isEmpty()) {
			Description description = (Description) context.umlToOml.get(element.getNearestPackage());
			Member type = context.getUmlOmlElementByName(element.eClass().getName());
			if (type instanceof Concept) {
				UMLConceptInstanceConverter.convert(element, type, context);
			} else if (type instanceof RelationEntity) {
				context.deferred.add(new UMLRelationConverter(element,(RelationEntity) type, description, context));
			}
		}
	}
}
