package io.opencaesar.papyrus2oml.converters;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class UMLLinkConverter extends LinkConverter {
	
	public UMLLinkConverter(Description description,String instanceIri, String relationIri, Object target , ConversionContext context ) {
		super(description,instanceIri,relationIri,target, context);
	}
	
	protected void createLink(Object value) {
		IdentifiedElement omlElement = getContext().umlToOml.get(value);
		if (omlElement==null) {
			return;
		}
		Ontology ont = OmlRead.getOntology(omlElement);
		String targetIri = OmlRead.getIri(omlElement);
		OMLUtil.addExtendsIfNeeded(getDescription(), ont.getIri(), getContext().writer);
		getContext().writer.addLinkAssertion(getDescription(), getInstanceIri(), getRelationIri(), targetIri);
	}

}
