package io.opencaesar.papyrus2oml.converters;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.uml2.uml.Package;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;
<<<<<<< HEAD
import io.opencaesar.papyrus2oml.util.UmlUtils;
=======
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965

public class PackageConverter {

	static public void convertPackage(Package package_, ConversionContext context) throws IOException {
		convertPackage(package_, "", context);
	}
	
	static public void convertPackage(Package package_, String postFix, ConversionContext context) throws IOException {
		boolean empty = package_.getPackagedElements().stream().filter(e -> !(e instanceof Package)).count() == 0;

		if (!empty) {
			final String prefix = package_.getName();
<<<<<<< HEAD
			String iri = UmlUtils.getIRI(package_);
=======
			final String iri = package_.getURI();
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
			final URI uri = URI.createURI(context.catalog.resolveURI(iri) + postFix + "." + OmlConstants.OML_EXTENSION);

			Description description = context.writer.createDescription(uri, iri, SeparatorKind.HASH, prefix);
			context.umlToOml.put(package_, description);

			context.writer.addDescriptionUsage(description, OmlConstants.OWL_IRI, null);

<<<<<<< HEAD
			DescriptionBundle bundle = context.descriptionBundle;
=======
			DescriptionBundle bundle = (DescriptionBundle) context.umlToOml.get(context.rootPackage);
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
			context.writer.addDescriptionBundleInclusion(bundle, iri, null);
		}
	}

	static public void convertRootPackage(Package package_, ConversionContext context) throws IOException {
		convertRootPackage(package_, "", context);
	}
	
	static public void convertRootPackage(Package package_,String postFix, ConversionContext context) throws IOException {
		final String prefix = package_.getName();
<<<<<<< HEAD
		String iri = UmlUtils.getIRI(package_);
		boolean empty = package_.getPackagedElements().stream().filter(e -> !(e instanceof Package)).count() == 0;
		iri = empty ? iri : iri + "-bunlde";
		final URI uri = URI.createURI(context.catalog.resolveURI(iri) +postFix +  "." + OmlConstants.OML_EXTENSION);
		DescriptionBundle bundle = context.writer.createDescriptionBundle(uri, iri, SeparatorKind.HASH, prefix);
		context.descriptionBundle = bundle;
		if (!empty) {
			convertPackage(package_,postFix, context);
		}
=======
		final String iri = package_.getURI();
		final URI uri = URI.createURI(context.catalog.resolveURI(iri) +postFix +  "." + OmlConstants.OML_EXTENSION);
		DescriptionBundle bundle = context.writer.createDescriptionBundle(uri, iri, SeparatorKind.HASH, prefix);
		context.umlToOml.put(package_, bundle);
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
	}


}
