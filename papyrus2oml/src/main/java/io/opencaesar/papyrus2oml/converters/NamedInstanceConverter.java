package io.opencaesar.papyrus2oml.converters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
<<<<<<< HEAD
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Package;
=======
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageableElement;
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
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
<<<<<<< HEAD
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class NamedInstanceConverter {

	static public void convert(Element element, ConversionContext context) throws IOException {
		String name =  UmlUtils.getName(element);
=======

public class NamedInstanceConverter {

	static public void convert(PackageableElement element, ConversionContext context) throws IOException {
		String name = element.getName();
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
		if (name == null || name.isEmpty()) {
			// Notice that some relations in UML could be anonymous
			// so we need to still handle them as unreified relations
			return;
		}
<<<<<<< HEAD
		
		if (name.equals("dronemodel")) {
			System.out.println("The one in questions");
		}
=======
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965

		Description description = (Description) context.umlToOml.get(element.getNearestPackage());
		List<Stereotype> stereotypes = element.getAppliedStereotypes();
		ResourceSet rs = description.eResource().getResourceSet();
		List<Member> types = new ArrayList<>();
		for (Stereotype s : stereotypes) {
			Package package_ = s.getNearestPackage();
<<<<<<< HEAD
			Import i = OMLUtil.addUsesIfNeeded(description, UmlUtils.getIRI(package_), context.writer);
=======
			Import i = OMLUtil.addUsesIfNeeded(description, package_.getURI(), context.writer);
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
			Member type = (Member) context.umlToOml.get(s);
			if (type == null) {
				URI uri = OmlRead.getResolvedImportUri(i);
				if (uri == null) {
<<<<<<< HEAD
					throw new RuntimeException("Cannot resolve IRI '" + UmlUtils.getIRI(package_) + "'");
=======
					throw new RuntimeException("Cannot resolve IRI '" + package_.getURI() + "'");
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
				}
				Resource r = rs.getResource(uri, true);
				Vocabulary vocabulary = (Vocabulary) OmlRead.getOntology(r);
				if (vocabulary == null) {
<<<<<<< HEAD
					throw new RuntimeException("Cannot load vocabylary '" + UmlUtils.getIRI(package_) + "'");
=======
					throw new RuntimeException("Cannot load vocabylary '" + package_.getURI() + "'");
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
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
			context.deferred.add(new RelationConverter(description,element, context, types,stereotypes));
		}
	}

}
