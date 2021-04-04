package io.opencaesar.papyrus2oml.converters;

import java.io.IOException;

import org.eclipse.uml2.uml.Element;

import io.opencaesar.oml.Concept;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class UMLNamedInstanceConverter {
	
	static public void convert(Element element, ConversionContext context) throws IOException {
		String name = UmlUtils.getName(element);
		if (name != null && !name.isEmpty()) {
			Description description = (Description) context.umlToOml.get(element.getNearestPackage());
			Member type = context.getUmlOmlElementByName(element.eClass().getName());
			if (type instanceof Concept) {
				UMLConceptInstanceConverter.convert(element, type, context);
			} else if (type instanceof RelationEntity) {
				context.deferred.add(new UMLRelationConverter(element,(RelationEntity) type, description, context));
			} else {
				context.logger.warn("Did not convert: " + element);
			}
		}else {
			context.logger.warn("Did not convert: " + element);
		}
	}
}
