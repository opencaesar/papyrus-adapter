package io.opencaesar.papyrus2oml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;
import io.opencaesar.papyrus2oml.util.DSLPackageConverter;
import io.opencaesar.papyrus2oml.util.ProfileConverter;
import io.opencaesar.papyrus2oml.util.ResourceConverter;
import io.opencaesar.papyrus2oml.util.UMLPackageConverter;

public class Papyrus2OmlConverter extends Ecore2OmlConverter {
	
	private ResourceSet rs;
	private List<String> ignoredIriPrefixes;
	private ConversionType conversionType = ConversionType.uml;

	public Papyrus2OmlConverter(File inputModelFile, List<String> ignoredIriPrefixes, OmlCatalog catalog, OmlWriter writer, ResourceSet omlResourceSet, ConversionType conversionType, Logger logger) {
		super(inputModelFile, catalog, writer, logger);
		this.rs = omlResourceSet;
		this.ignoredIriPrefixes = ignoredIriPrefixes;
		this.conversionType = conversionType;
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
				converters.add(new ProfileConverter((Profile)root, catalog, writer,conversionType, logger));
			} else if (root instanceof Package) {
				if (conversionType == ConversionType.uml || conversionType == ConversionType.uml_dsl) {
					converters.add(new UMLPackageConverter((Package)root, ignoredIriPrefixes, catalog, writer, rs,conversionType, logger));
				}
				if (conversionType == ConversionType.dsl || conversionType == ConversionType.uml_dsl) {
					Package rootPackage = (Package) root; 
					Model model = rootPackage.getModel();
					EList<Profile> profiles = model.getAllAppliedProfiles();
					if (!profiles.isEmpty()) {
						converters.add(new DSLPackageConverter(rootPackage, profiles.get(0), ignoredIriPrefixes, catalog, writer, rs,conversionType, logger));
					}
				}
			}
		}
		return converters;
	}
	
}
