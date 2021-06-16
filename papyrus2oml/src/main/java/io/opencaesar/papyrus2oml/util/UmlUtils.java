/**
 * 
 * Copyright 2021 Modelware Solutions and CAE-LIST.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package io.opencaesar.papyrus2oml.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Ontology;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class UmlUtils {

	private static final String NAME = "name";
	private static final String IRI = "iri";
	private static final String OMLIRI = "http://io.opencaesar.oml/omliri";

	public static final String UML_IRI = "http://www.eclipse.org/uml2/5.0.0/UML";
	public static final String UML_NS = UML_IRI+"#";
	public static final String UML_BUNDLE_IRI = UML_IRI+"-Bundle";
	
	static public String getIri(Property property) {
		EAnnotation annotation = property.getEAnnotation(OMLIRI);
		if (annotation!=null) {
			return annotation.getDetails().get(IRI);
		}
		return "";
	}

	public static String getIRI(Package package_) {
		String iri = package_.getURI();
		if (iri==null || iri.isEmpty()) {
			iri = "http://" + package_.getQualifiedName().replaceAll("::", "/").replaceAll(" ", "_");
		}
		return iri;
	}
	
	public static String getIRI(Element element, ConversionContext context) {
		Package pkg = element.getNearestPackage();
		Ontology ontology = (Ontology) context.umlToOml.get(pkg);
		return ontology.getNamespace() + UmlUtils.getName(element);
	}
	
	public static String getIRI(Description description, Element element) {
		return description.getNamespace() + UmlUtils.getName(element);
	}
	
	
	private static String _getID(Element element) {
		Resource res = element.eResource();
		if (res instanceof XMLResource) {
			return ((XMLResource)res).getID(element);
		}
		return "";
	}
	
	static public String getOmlName(NamedElement element) {
		String name = null;
		EAnnotation annotation = element.getEAnnotation(OMLIRI);
		if (annotation!=null) {
			name = annotation.getDetails().get(NAME);
		}
		return name !=null ? name : element.getName();
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
			name = name.replaceAll("&", "_"); // TODO: replace other unexpected chars
			if (!(element instanceof Package)) {
				names.add(name);
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

	public static String getUMLIRI(Element element, ConversionContext context) {
		Package pkg = element.getNearestPackage();
		IdentifiedElement oPkg = context.umlToOml.get(pkg);
		Ontology ontology = oPkg.getOntology();
		String ns = ontology.getNamespace();
		if (context.postFix!=null) {
			String end = "" + ns.charAt(ns.length()-1);
			ns = ns.substring(0,ns.length()-(context.postFix.length()+2)) + end;
		}
		return ns + UmlUtils.getName(element);
	}

	public static String getUMLONTIRI(Element element, ConversionContext context) {
		Package pkg = element.getNearestPackage();
		IdentifiedElement oPkg = context.umlToOml.get(pkg);
		Ontology ontology = oPkg.getOntology();
		String ns = ontology.getNamespace();
		if (context.postFix!=null) {
			ns = ns.substring(0,ns.length()-(context.postFix.length()+2));
		}
		return ns;
	}
	
	public static String getOntIRI(String iri) {
		int i = iri.lastIndexOf('#');
		String baseIri;
		if (i > 0) {
			baseIri = iri.substring(0, i);			
		} else {
			i = iri.lastIndexOf('/');
			baseIri = iri.substring(0, i);			
		}
		return baseIri;
	}

}
