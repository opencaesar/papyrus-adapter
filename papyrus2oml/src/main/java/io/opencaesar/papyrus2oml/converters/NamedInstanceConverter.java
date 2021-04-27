package io.opencaesar.papyrus2oml.converters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Stereotype;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Import;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.papyrus2oml.util.OMLUtil;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class NamedInstanceConverter {

	static public void convert(Element element, ConversionContext context) throws IOException {
		String name =  UmlUtils.getName(element);
		if (name == null || name.isEmpty()) {
			// Notice that some relations in UML could be anonymous
			// so we need to still handle them as unreified relations
			return;
		}
		Description description = (Description) context.umlToOml.get(element.getNearestPackage());
		if (description == null) {
			// this means the element is owned by the root model
			// which is mapped to a description bundle not a description
			return;
		}

		ResourceSet rs = description.eResource().getResourceSet();
		List<Member> types = new ArrayList<>();
		List<Stereotype> stereotypes = element.getAppliedStereotypes();
		for (Stereotype s : stereotypes) {
			Package package_ = s.getNearestPackage();
			Import i = OMLUtil.addUsesIfNeeded(description, UmlUtils.getIRI(package_), context.writer);
			Member type = (Member) context.umlToOml.get(s);
			if (type == null) {
				URI uri = OmlRead.getResolvedImportUri(i);
				if (uri == null) {
					throw new RuntimeException("Cannot resolve IRI '" + UmlUtils.getIRI(package_) + "'");
				}
				Resource r = rs.getResource(uri, true);
				Vocabulary vocabulary = (Vocabulary) OmlRead.getOntology(r);
				if (vocabulary == null) {
					throw new RuntimeException("Cannot load vocabylary '" + UmlUtils.getIRI(package_) + "'");
				}
				type = OmlRead.getMemberByName(vocabulary, s.getName());
				if (type == null) {
					throw new RuntimeException("Cannot find entity equivalent to '" + s.getQualifiedName() + "'");
				}
				context.umlToOml.put(s, type);
			}
			types.add(type);
		}
				
		Member type = types.stream().filter(t -> !(t instanceof Aspect)).findFirst().orElse(null);
		if (type instanceof Concept) {
			ConceptInstanceConverter.convert(element, description,stereotypes, types, context);
		} else if (type instanceof RelationEntity) {
			context.deferredRelations.add(new RelationConverter(description,element, context, types,stereotypes));
		}
	}

}
