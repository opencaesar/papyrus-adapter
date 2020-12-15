package io.opencaesar.papyrus2oml;

import java.io.File;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.ProfileApplication;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;

public class Papyrus2OmlConverter extends Ecore2OmlConverter {

	private static final String UML_IRI = "http://www.eclipse.org/uml2/5.0.0/UML";
	
	public Papyrus2OmlConverter(File inputFolder, File inputModelFile, ResourceSet omlResourceSet, OmlCatalog catalog, OmlWriter writer, Logger logger) {
		super(inputFolder, inputModelFile, omlResourceSet, catalog, writer, logger);
	}

	protected ResourceSet createInputResourceSet() {
		ResourceSet rs = new ResourceSetImpl();
		UMLResourcesUtil.init(rs);
		return rs;
	}

	protected boolean shouldBeConverted(EObject eObject) {
		return eObject.eClass().getEPackage().getNsURI().equals(UML_IRI) &&
			!(eObject instanceof ProfileApplication);
	}

}
