package io.opencaesar.papyrus2oml.converters;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.uml2.uml.Package;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class PackageConverter {

	static public void convertPackage(Package package_, ConversionContext context) throws IOException {
		boolean empty = package_.getPackagedElements().stream().filter(e -> !(e instanceof Package)).count() == 0;

		if (!empty) {
			final String prefix = package_.getName();
			final String iri = package_.getURI();
			final URI uri = URI.createURI(context.catalog.resolveURI(iri) + "." + OmlConstants.OML_EXTENSION);

			Description description = context.writer.createDescription(uri, iri, SeparatorKind.HASH, prefix);
			context.umlToOml.put(package_, description);

			context.writer.addDescriptionUsage(description, OmlConstants.OWL_IRI, null);

			DescriptionBundle bundle = (DescriptionBundle) context.umlToOml.get(context.rootPackage);
			context.writer.addDescriptionBundleInclusion(bundle, iri, null);
		}
	}

	static public void convertRootPackage(Package package_, ConversionContext context) throws IOException {
		final String prefix = package_.getName();
		final String iri = package_.getURI();
		final URI uri = URI.createURI(context.catalog.resolveURI(iri) + "." + OmlConstants.OML_EXTENSION);
		DescriptionBundle bundle = context.writer.createDescriptionBundle(uri, iri, SeparatorKind.HASH, prefix);
		context.umlToOml.put(package_, bundle);
	}

}
