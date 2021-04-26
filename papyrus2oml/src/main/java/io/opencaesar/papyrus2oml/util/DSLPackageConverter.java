package io.opencaesar.papyrus2oml.util;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Profile;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.converters.NamedInstanceConverter;
import io.opencaesar.papyrus2oml.converters.PackageConverter;

public class DSLPackageConverter extends ResourceConverter {
	
	public DSLPackageConverter(Package rootPackage, Profile profile, List<String> ignoredIriPrefixes, OmlCatalog catalog,
			OmlWriter writer, ResourceSet rs, ConversionType conversionType, Logger logger) {
		super(new ConversionContext(ignoredIriPrefixes, catalog, writer, rs,conversionType, logger));
		context.rootPackage = rootPackage;
		context.DSL =true;
		logger.info("DLS converter in : " + (conversionType==ConversionType.dsl? " DSL mode" : "UML-DSL model"));
		if (conversionType==ConversionType.uml_dsl) {
			context.postFix = profile.getName();
		}
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
		context.logger.info("Reations Conversion: ");
		context.deferred.forEach(r -> r.run());
	}

	@Override
	public void convertEObject(EObject eObject) throws IOException {
		if (eObject == context.rootPackage) {
			PackageConverter.convertRootPackage(context.rootPackage,context.postFix, context);
		} else if (eObject instanceof Package) {
			PackageConverter.convertPackage((Package) eObject,context.postFix, context);
		} else if (eObject instanceof Element) {
			NamedInstanceConverter.convert((Element) eObject, context);
		} else {
			context.logger.warn("Not Converted : " + eObject.eClass().getName());
		}
	}
}