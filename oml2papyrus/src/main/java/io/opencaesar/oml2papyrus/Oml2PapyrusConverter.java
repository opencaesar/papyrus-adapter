package io.opencaesar.oml2papyrus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlRead;

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
}
