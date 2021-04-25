package io.opencaesar.papyrus2oml.util;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.ElementImport;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageImport;
import org.eclipse.uml2.uml.ProfileApplication;
import org.eclipse.uml2.uml.Slot;
import org.eclipse.uml2.uml.ValueSpecification;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.converters.PackageConverter;
import io.opencaesar.papyrus2oml.converters.UMLNamedInstanceConverter;

public class UMLPackageConverter extends ResourceConverter {
	
	public UMLPackageConverter(Package rootPackage, List<String> ignoredIriPrefixes, OmlCatalog catalog, OmlWriter writer, ResourceSet omlResourceSet, ConversionType conversionType, Logger logger) {
		super(new ConversionContext(ignoredIriPrefixes, catalog, writer, omlResourceSet,conversionType, logger));
		context.rootPackage = rootPackage;
		logger.info("UML converter in : " + (conversionType==ConversionType.uml? " UML mode" : "UML-DSL model"));
	}
	
	@Override
	public boolean shouldBeIgnored(EObject eObject) {
		if (eObject instanceof Package) {
			Package pkg = (Package) eObject;
			String iri = UmlUtils.getIRI(pkg);
			if (OMLUtil.shouldIgnoreIri(context.ignoredIriPrefixes, iri)) {
				return true;
			}
		}
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
			PackageConverter.convertPackage((Package)eObject,context);
		} else if (eObject instanceof Element) {
			UMLNamedInstanceConverter.convert((Element)eObject,context);
		} else {
			System.out.println("Not Converted : " + eObject.eClass().getName());
		}
	}
}