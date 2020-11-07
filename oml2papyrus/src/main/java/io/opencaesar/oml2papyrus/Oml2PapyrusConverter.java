package io.opencaesar.oml2papyrus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;

import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml2papyrus.util.StereotypeUtils;

public class Oml2PapyrusConverter {

	private Ontology rootOntology;
	private OmlCatalog catalog;
	private File papyrusFolder;
	private ResourceSet papyrusResourceSet;
	private Logger logger;
	
	public Oml2PapyrusConverter(Ontology rootOntology, OmlCatalog catalog, File papyrusFolder, ResourceSet papyrusResourceSet, Logger logger) {
		this.rootOntology = rootOntology;
		this.catalog = catalog;
		this.papyrusFolder = papyrusFolder;
		this.papyrusResourceSet = papyrusResourceSet;
		this.logger = logger;
	}

	public List<Resource> convert() throws Exception {
		// load the relevant ontologies
		List<Ontology> allOntologies = OmlRead.getAllImportedOntologiesInclusive(rootOntology);
		
		// collect the papyrus resources
		List<Resource> papyrusResources = new ArrayList<>();
		
		// Create the papyrus resource
		// ...
		
		// return the created Papyrus resources
		return papyrusResources;
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println("Test");
		Model uml2Model = StereotypeUtils.getUML2Model();
		System.out.println(uml2Model.getName());
		final Profile sampleProfile = UMLFactory.eINSTANCE.createProfile();
		sampleProfile.setName("Sample Profile");
		final Stereotype testStereoType = 
				StereotypeUtils.createStereotype(sampleProfile,
												 "TestStereoType",
												 false,
												 uml2Model,
												 UMLPackage.Literals.CLASS.getName(),
												 UMLPackage.Literals.GENERALIZATION.getName());
		System.out.println(testStereoType.getExtendedMetaclasses());
		ResourceSet rSet = new ResourceSetImpl();
		Resource profileResrouce = rSet.createResource(URI.createFileURI("testProfile.uml"));
		profileResrouce.getContents().add(sampleProfile);
		profileResrouce.save(null);
	}
}
