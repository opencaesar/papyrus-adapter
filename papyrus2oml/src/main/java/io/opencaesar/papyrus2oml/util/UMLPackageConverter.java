package io.opencaesar.papyrus2oml.util;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
<<<<<<< HEAD
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.ElementImport;
=======
import org.eclipse.uml2.uml.Element;
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageImport;
import org.eclipse.uml2.uml.ProfileApplication;
import org.eclipse.uml2.uml.Slot;
import org.eclipse.uml2.uml.ValueSpecification;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;
import io.opencaesar.papyrus2oml.converters.PackageConverter;
import io.opencaesar.papyrus2oml.converters.UMLNamedInstanceConverter;

public class UMLPackageConverter extends ResourceConverter {
	
	public UMLPackageConverter(Package rootPackage, OmlCatalog catalog, OmlWriter writer,ResourceSet omlResourceSet, Logger logger) {
		super(new ConversionContext(catalog, writer,omlResourceSet, logger));
		context.rootPackage = rootPackage;
	}
	
	@Override
	public boolean shouldBeIgnored(EObject eObject) {
		if (!(eObject instanceof Element)) {
			return true;
		}
		return false;
	}
	
	@Override
	public void finish() {
		context.deferred.forEach(r -> r.run());
	}

	@Override
	public void convertEObject(EObject eObject) throws IOException {
		if (eObject == context.rootPackage) {
			PackageConverter.convertRootPackage(context.rootPackage, "-uml", context);
		} else if (eObject instanceof Package) {
			PackageConverter.convertPackage((Package)eObject,"-uml",context);
<<<<<<< HEAD
		} else if (eObject instanceof Comment) {
			// 
		} else if (eObject instanceof ValueSpecification) {
			// 
		} else if (eObject instanceof Slot) {
			// 
		} else if (eObject instanceof ProfileApplication ||
				  eObject instanceof PackageImport ||
				  eObject instanceof ElementImport) {
			// 
		} else if (eObject instanceof Element) {
			System.out.println(UmlUtils.getName((Element)eObject));
			UMLNamedInstanceConverter.convert((Element)eObject,context);
		} else {
			System.out.println("Not Converted : " + eObject.eClass().getName());
=======
		} else if (eObject instanceof PackageableElement) {
			UMLNamedInstanceConverter.convert((PackageableElement)eObject,context);
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
		}
	}
}