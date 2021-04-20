package io.opencaesar.oml2papyrus.util;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;

public class UmlUtils {
	
	private static final String IRI_VALUE = "iri_value";
	private static final String OMLIRI = "http://io.opencaesar.oml/omliri";
	
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
	
	static public void setIRIAnnotation(Property umlProperty, String iri) {
		EAnnotation annotatoin = EcoreFactory.eINSTANCE.createEAnnotation();
		annotatoin.setSource(OMLIRI);
		annotatoin.getDetails().put(IRI_VALUE, iri);
		umlProperty.getEAnnotations().add(annotatoin);
	}
}
