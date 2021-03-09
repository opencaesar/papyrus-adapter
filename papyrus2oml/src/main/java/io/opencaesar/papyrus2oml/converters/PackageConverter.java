package io.opencaesar.papyrus2oml.converters;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.uml2.uml.Package;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
import io.opencaesar.papyrus2oml.util.UmlUtils;

public class PackageConverter {

	static public void convertPackage(Package package_, ConversionContext context) throws IOException {
		convertPackage(package_, "", context);
	}
	
	static public void convertPackage(Package package_, String postFix, ConversionContext context) throws IOException {
		boolean empty = package_.getPackagedElements().stream().filter(e -> !(e instanceof Package)).count() == 0;

		if (!empty) {
			final String prefix = package_.getName();
			String iri = UmlUtils.getIRI(package_) + postFix;
			final URI uri = URI.createURI(context.catalog.resolveURI(iri) + "." + OmlConstants.OML_EXTENSION);

			Description description = context.writer.createDescription(uri, iri, SeparatorKind.HASH, prefix);
			context.umlToOml.put(package_, description);

			context.writer.addDescriptionUsage(description, OmlConstants.OWL_IRI, null);
			DescriptionBundle bundle = context.descriptionBundle;
			context.writer.addDescriptionBundleInclusion(bundle, iri, null);
		}
	}

	static public void convertRootPackage(Package package_, ConversionContext context) throws IOException {
		convertRootPackage(package_, "", context);
	}
	
	static public void convertRootPackage(Package package_,String postFix, ConversionContext context) throws IOException {
		final String prefix = package_.getName();
		String iri = UmlUtils.getIRI(package_) + postFix;
		boolean empty = package_.getPackagedElements().stream().filter(e -> !(e instanceof Package)).count() == 0;
		iri = empty ? iri : iri + "-bundle";
		final URI uri = URI.createURI(context.catalog.resolveURI(iri) +   "." + OmlConstants.OML_EXTENSION);
		DescriptionBundle bundle = context.writer.createDescriptionBundle(uri, iri, SeparatorKind.HASH, prefix);
		context.descriptionBundle = bundle;
		if (!empty) {
			convertPackage(package_,postFix, context);
		}
	}
}
