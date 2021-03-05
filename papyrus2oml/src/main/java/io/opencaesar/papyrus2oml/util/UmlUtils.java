package io.opencaesar.papyrus2oml.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;

public class UmlUtils {

	public static final String UML_IRI = "http://www.eclipse.org/uml2/5.0.0/UML";
	
	public static final String UML_NS = UML_IRI+"#";
<<<<<<< HEAD
	
	public static String getIRI(Package package_) {
		String iri = package_.getURI();
		if (iri==null || iri.isEmpty()) {
			iri = "http://" + package_.getQualifiedName().replaceAll("::", "/").replaceAll(" ", "_");
		}
		return iri;
	}
	
	
	private static String _getID(Element element) {
		Resource res = element.eResource();
		if (res instanceof XMLResource) {
			return ((XMLResource)res).getID(element);
		}
		return "";
	}
	
	private static void getQualifiedNames(Element element, List<String> names) {
		String name = null;
		if (element instanceof NamedElement) {
			name = ((NamedElement)element).getName();
		}
		if (name==null || name.isEmpty()) {
			name = _getID(element);
			names.add(name);
		}else {
			names.add(name);
			if (!(element instanceof Package)) {
				getQualifiedNames(element.getOwner(), names);
			}
		}
	}
	
	public static String getName(Element element) {
		List<String> names = new ArrayList<>();
		getQualifiedNames(element, names);
		StringBuilder qName = new StringBuilder();
		for (int index = names.size()-1 ; index >=1 ; index--) {
			qName.append(names.get(index)).append("_");
		}
		qName.append(names.get(0));
		return qName.toString();
	}
=======

>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
}
