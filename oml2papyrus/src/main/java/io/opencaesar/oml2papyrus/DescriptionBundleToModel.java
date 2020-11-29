package io.opencaesar.oml2papyrus;

import java.io.File;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import io.opencaesar.oml.DescriptionBundle;

public class DescriptionBundleToModel {

	private DescriptionBundle rootOntology;
	private File papyrusFolder;
	private ResourceSet papyrusResourceSet;
	private Logger logger;
	
	public DescriptionBundleToModel(DescriptionBundle rootOntology, File papyrusFolder, ResourceSet papyrusResourceSet, Logger logger) {
		this.rootOntology = rootOntology;
		this.papyrusFolder = papyrusFolder;
		this.papyrusResourceSet = papyrusResourceSet;
		this.logger = logger;
	}

	public Resource convert() throws Exception {
		return null;
	}

}
