package io.opencaesar.papyrus2oml.util;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionExtension;
import io.opencaesar.oml.DescriptionUsage;
import io.opencaesar.oml.Import;
import io.opencaesar.oml.util.OmlWriter;

public class OMLUtil {

	static public DescriptionUsage addUsesIfNeeded(Description description, String iri, OmlWriter writer) {
		for (Import i : description.getOwnedImports()) {
			if (i instanceof DescriptionUsage && i.getUri().equals(iri)) {
				return (DescriptionUsage) i;
			}
		}
		return writer.addDescriptionUsage(description, iri, null);
	}

	static public DescriptionExtension addExtendsIfNeeded(Description description, String iri, OmlWriter writer) {
		for (Import i : description.getOwnedImports()) {
			if (i instanceof DescriptionExtension && i.getUri().equals(iri)) {
				return (DescriptionExtension) i;
			}
		}
		return writer.addDescriptionExtension(description, iri, null);
	}

}
