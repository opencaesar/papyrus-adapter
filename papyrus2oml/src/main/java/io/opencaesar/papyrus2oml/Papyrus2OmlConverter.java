package io.opencaesar.papyrus2oml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;
import io.opencaesar.papyrus2oml.util.DSLPackageConverter;
import io.opencaesar.papyrus2oml.util.ProfileConverter;
import io.opencaesar.papyrus2oml.util.ResourceConverter;

public class Papyrus2OmlConverter extends Ecore2OmlConverter {
	
	private ResourceSet rs;

	public Papyrus2OmlConverter(File inputModelFile, OmlCatalog catalog, OmlWriter writer, ResourceSet omlResourceSet, Logger logger) {
		super(inputModelFile, catalog, writer, logger);
		this.rs = omlResourceSet;
	}

	@Override
	protected ResourceSet createInputResourceSet() {
		ResourceSet rs = super.createInputResourceSet();
		UMLResourcesUtil.init(rs);
		return rs;
	}

	@Override
	public Collection<ResourceConverter> getResourceConverters(Resource resource) throws IOException {
		List<ResourceConverter> converters = new ArrayList<>();
		if (!resource.getContents().isEmpty()) {
			EObject root = resource.getContents().get(0);
			if (root instanceof Profile) {
				converters.add(new ProfileConverter((Profile)root, catalog, writer, logger));
			} else if (root instanceof Package) {
				converters.add(new DSLPackageConverter((Package)root, catalog, writer,rs, logger));
				//converters.add(new UMLPackageConverter((Package)root, catalog, writer, logger));
			}
		}
		return converters;
	}
	
}
