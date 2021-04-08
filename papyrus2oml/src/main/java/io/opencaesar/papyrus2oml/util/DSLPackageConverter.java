package io.opencaesar.papyrus2oml.util;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Profile;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;
import io.opencaesar.papyrus2oml.ConversionType;
import io.opencaesar.papyrus2oml.converters.NamedInstanceConverter;
import io.opencaesar.papyrus2oml.converters.PackageConverter;

public class DSLPackageConverter extends ResourceConverter {
	
	public DSLPackageConverter(Package rootPackage, List<String> ignoredIriPrefixes, OmlCatalog catalog,
			OmlWriter writer, ResourceSet rs, ConversionType conversionType, Logger logger) {
		super(new ConversionContext(ignoredIriPrefixes, catalog, writer, rs,conversionType, logger));
		context.rootPackage = rootPackage;
		context.DSL =true;
		logger.info("DLS converter in : " + (conversionType==ConversionType.DSL? " DSL mode" : "UML-DSL model"));
		if (conversionType==ConversionType.UML_DSL) {
			Model model = rootPackage.getModel();
			EList<Profile> profiles = model.getAllAppliedProfiles();
			// is it valid to just use the first ?
			context.postFix = profiles.get(0).getLabel();
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
		System.out.println("");
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
			System.out.println("Not Converted : " + eObject.eClass().getName());
		}
	}
}