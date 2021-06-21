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
package io.opencaesar.oml2papyrus.util;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;

public class UmlUtils {

	public static final String TYPES_IRI = "http://www.eclipse.org/uml2/5.0.0/Types";
	public static final String UML_IRI = "http://www.eclipse.org/uml2/5.0.0/UML";

	private static final String IRI = "iri";
	private static final String NAME = "name";
	private static final String OMLIRI = "http://io.opencaesar.oml/omliri";
	
	public static void addNameAnnotationIfNeeded(NamedElement element) {
		String name = element.getName(); 
		String friendlyName = getUMLFirendlyName(name);
        if (!name.equals(friendlyName)) {
        	 element.setName(friendlyName);
        	 setAnnotation(element, NAME, name);
        }
	}

	public static String getUMLFirendlyName(String name) {
		 char ch = name.charAt(0);
        if(Character.isDigit(ch)) {
       	 return "_" + name;
        }
        return name;
	}

	public static Model createModel(String modelName, String modelURI) {
		Model model = UMLFactory.eINSTANCE.createModel();
		model.setName(modelName);
		model.setURI(modelURI);
		return model;
	}

	public static Package getPackage(String iri, Package pkg) {
		int i = iri.lastIndexOf("/");
		if (iri.length() == i+1) {
			return pkg;
		}
		if (i > 0) {
			pkg = getPackage(iri.substring(0, i), pkg);
		}
		String name = iri.substring(i+1);
		Package newPkg = (Package) pkg.getPackagedElement(name, false, UMLPackage.Literals.PACKAGE, false);
		if (newPkg == null) {
			newPkg = ProfileUtils.createPackage(pkg, name, iri);
		}
		return newPkg;
	}
	
	static public void addIRIAnnotation(Property umlProperty, String iri) {
		setAnnotation(umlProperty, IRI, iri);
	}

	static public void setAnnotation(EModelElement element, String key, String value) {
		EAnnotation annotatoin = EcoreFactory.eINSTANCE.createEAnnotation();
		annotatoin.setSource(OMLIRI);
		annotatoin.getDetails().put(key, value);
		element.getEAnnotations().add(annotatoin);
	}

}
