package io.opencaesar.papyrus2oml.converters;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;

import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class ConceptInstanceConverter {
	
	private static final String IRI_VALUE = "iri_value";
	private static final String OMLIRI = "http://io.opencaesar.oml/omliri";

	static public void convert(PackageableElement element, Description description, List<Stereotype> stereotypes, List<Member> types,
			ConversionContext context) {
		ConceptInstance instance = context.writer.addConceptInstance(description, element.getName());
		context.umlToOml.put(element, instance);
		String instanceIri = OmlRead.getIri(instance);
		for (Member t : types) {
			context.writer.addConceptTypeAssertion(description, instanceIri, OmlRead.getIri(t));
		}
	
		// get the stereoType applications
		for (Stereotype stereoType : stereotypes) {
			EObject stApplication = element.getStereotypeApplication(stereoType);
			EClass eClass = stApplication.eClass();
			EList<Property> props = stereoType.allAttributes();
			for (Property prop : props) {
				if (context.shouldFilterFeature(prop)) {
					continue;
				}
				EStructuralFeature feature = eClass.getEStructuralFeature(prop.getName());
				if (feature==null) {
					continue;
				}
				Object val = stApplication.eGet(feature);
				if (prop.isMultivalued()) {
					EList<?> values = (EList<?>) val;
					if (!values.isEmpty()) {
						String propIRI = getIri(prop);
						if (propIRI.isEmpty()) {
							context.logger.error("Could not get IRI for " + prop.getName());
							continue;
						}
						if (feature instanceof EAttribute) {
							for (Object value  : values) {
								Literal literal = context.getLiteralValue(description, value);
								context.writer.addScalarPropertyValueAssertion(description, instanceIri, propIRI, literal);
							}
						}else {
							context.deferred.add(new LinkConverter(description, instanceIri, propIRI, val, context ));
						}
					}
				}else if (val!=null) {
					String propIRI = getIri(prop);
					if (propIRI.isEmpty()) {
						context.logger.error("Could not get IRI for " + prop.getName());
						continue;
					}
					if (feature instanceof EAttribute) {
						Literal literal = context.getLiteralValue(description, val);
						context.writer.addScalarPropertyValueAssertion(description, instanceIri, propIRI, literal);
					}else {
						context.deferred.add(new LinkConverter(description, instanceIri, propIRI, val, context ));
					}
				}
				
			}
		}
		
		if (element instanceof Classifier) {
			Classifier clazz = (Classifier)element;
			EList<Property> props = clazz.getAttributes();
			for (Property prop : props) {
				System.out.println(prop.getLabel());
			}
		}
	}
	
	static public String getIri(Property prop) {
		EAnnotation annotation = prop.getEAnnotation(OMLIRI);
		if (annotation!=null) {
			return annotation.getDetails().get(IRI_VALUE);
		}
		return "";
	}

}
