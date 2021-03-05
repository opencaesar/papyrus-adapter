package io.opencaesar.papyrus2oml.util;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Package;
<<<<<<< HEAD
=======
import org.eclipse.uml2.uml.PackageableElement;
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;
import io.opencaesar.papyrus2oml.converters.NamedInstanceConverter;
import io.opencaesar.papyrus2oml.converters.PackageConverter;

public class DSLPackageConverter extends ResourceConverter {

	public DSLPackageConverter(Package rootPackage, OmlCatalog catalog, OmlWriter writer, ResourceSet rs, Logger logger) {
		super(new ConversionContext(catalog, writer,rs, logger));
		context.rootPackage = rootPackage;
	}

	@Override
	public boolean shouldBeIgnored(EObject eObject) {
		return !(eObject instanceof Element);
	}

	@Override
	public void finish() {
		context.deferred.forEach(r -> r.run());
	}

	@Override
	public void convertEObject(EObject eObject) throws IOException {
		if (eObject == context.rootPackage) {
			PackageConverter.convertRootPackage(context.rootPackage, context);
		} else if (eObject instanceof Package) {
			PackageConverter.convertPackage((Package) eObject, context);
<<<<<<< HEAD
		} else if (eObject instanceof Element) {
			NamedInstanceConverter.convert((Element) eObject, context);
=======
		} else if (eObject instanceof PackageableElement) {
			NamedInstanceConverter.convert((PackageableElement) eObject, context);
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
		} else {
			System.out.println("Not Converted : " + eObject.eClass().getName());
		}
	}
}