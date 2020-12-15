package io.opencaesar.oml2papyrus.util;

import java.util.List;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Dependency;
import org.eclipse.uml2.uml.DirectedRelationship;
import org.eclipse.uml2.uml.ElementImport;
import org.eclipse.uml2.uml.Extend;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.Include;
import org.eclipse.uml2.uml.InformationFlow;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Namespace;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageImport;
import org.eclipse.uml2.uml.PackageMerge;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.ProfileApplication;
import org.eclipse.uml2.uml.ProtocolConformance;
import org.eclipse.uml2.uml.ProtocolStateMachine;
import org.eclipse.uml2.uml.TemplateBinding;
import org.eclipse.uml2.uml.TemplateSignature;
import org.eclipse.uml2.uml.TemplateableElement;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UseCase;

public class UmlUtils {
	
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

	public static void setSources(DirectedRelationship relationship, List<NamedElement> elements) {
		if (relationship instanceof Dependency) {
			((Dependency)relationship).getClients().addAll(elements);
		} else if (relationship instanceof ElementImport) {
			((ElementImport)relationship).setImportingNamespace((Namespace)elements.get(0));
		} else if (relationship instanceof Extend) {
			((Extend)relationship).setExtension((UseCase)elements.get(0));
		} else if (relationship instanceof Generalization) {
			((Generalization)relationship).setSpecific((Classifier)elements.get(0));
		} else if (relationship instanceof Include) {
			((Include)relationship).setIncludingCase((UseCase)elements.get(0));
		} else if (relationship instanceof InformationFlow) {
			((InformationFlow)relationship).getInformationSources().addAll(elements);
		} else if (relationship instanceof PackageImport) {
			((PackageImport)relationship).setImportingNamespace((Namespace)elements.get(0));
		} else if (relationship instanceof PackageMerge) {
			((PackageMerge)relationship).setReceivingPackage((Package)elements.get(0));
		} else if (relationship instanceof ProfileApplication) {
			((ProfileApplication)relationship).setApplyingPackage((Package)elements.get(0));
		} else if (relationship instanceof ProtocolConformance) {
			((ProtocolConformance)relationship).setSpecificMachine((ProtocolStateMachine)elements.get(0));
		} else if (relationship instanceof TemplateBinding) {
			((TemplateBinding)relationship).setBoundElement((TemplateableElement)elements.get(0));
		}
	}
	
	public static void setTargets(DirectedRelationship relationship, List<NamedElement> elements) {
		if (relationship instanceof Dependency) {
			((Dependency)relationship).getSuppliers().addAll(elements);
		} else if (relationship instanceof ElementImport) {
			((ElementImport)relationship).setImportedElement((PackageableElement)elements.get(0));
		} else if (relationship instanceof Extend) {
			((Extend)relationship).setExtendedCase((UseCase)elements.get(0));
		} else if (relationship instanceof Generalization) {
			((Generalization)relationship).setGeneral((Classifier)elements.get(0));
		} else if (relationship instanceof Include) {
			((Include)relationship).setAddition((UseCase)elements.get(0));
		} else if (relationship instanceof InformationFlow) {
			((InformationFlow)relationship).getInformationTargets().addAll(elements);
		} else if (relationship instanceof PackageImport) {
			((PackageImport)relationship).setImportedPackage((Package)elements.get(0));
		} else if (relationship instanceof PackageMerge) {
			((PackageMerge)relationship).setMergedPackage((Package)elements.get(0));
		} else if (relationship instanceof ProfileApplication) {
			((ProfileApplication)relationship).setAppliedProfile((Profile)elements.get(0));
		} else if (relationship instanceof ProtocolConformance) {
			((ProtocolConformance)relationship).setGeneralMachine((ProtocolStateMachine)elements.get(0));
		} else if (relationship instanceof TemplateBinding) {
			((TemplateBinding)relationship).setSignature((TemplateSignature)elements.get(0));
		}
	}

}
