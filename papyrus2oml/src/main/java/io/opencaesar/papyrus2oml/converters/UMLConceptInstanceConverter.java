package io.opencaesar.papyrus2oml.converters;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class UMLConceptInstanceConverter {
	
	public static void convert(PackageableElement element,Member type,ConversionContext context ) {
		// attributes in this case are the properties of the element.eClass
		Description description = (Description) context.umlToOml.get(element.getNearestPackage());
		ConceptInstance instance = context.writer.addConceptInstance(description, element.getName());
		String instanceIRI = OmlRead.getIri(instance);
		context.writer.addConceptTypeAssertion(description, instanceIRI, OmlRead.getIri(type));
		context.umlToOml.put(element, instance);
		OMLUtil.addExtendsIfNeeded(description,  OmlRead.getOntology(type).getIri(), context.writer);	
		EClass umlclass = element.eClass();
		EList<EAttribute> attrs = umlclass.getEAllAttributes();
		for (EAttribute feature : attrs) {
			if (context.shouldFilterFeature(feature)) {
				continue;
			}
			Object val = element.eGet(feature);
			Object dVal = feature.getDefaultValue();
			if (feature.isMany()) {
				EList<?> values = (EList<?>) val;
				if (!values.isEmpty()) {
					String propIRI = getIri(feature);
					for (Object value  : values) {
						// TODO: handle structure
						Literal literal = context.getLiteralValue(description, value);
						context.writer.addScalarPropertyValueAssertion(description,  instanceIRI, propIRI, literal);
					}
				}
			}else if (val!=null) {
				String propIRI = getIri(feature);
				Literal literal = context.getLiteralValue(description, val);
				context.writer.addScalarPropertyValueAssertion(description, instanceIRI, propIRI, literal);
			}
		}
	}

	private static String getIri(EAttribute feature) {
		return UmlUtils.UML_NS + StringExtensions.toFirstLower(feature.getContainerClass().getSimpleName()) + "_" + feature.getName();
	}

}
