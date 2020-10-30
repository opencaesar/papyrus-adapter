package io.opencaesar.papyrus2oml;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;

public class Papyrus2OmlConverter {

	private static final String OML_EXTENSION = "oml";

	private File inputFolder;
	private File inputFile;
	private OmlCatalog catalog;
	private OmlWriter writer;
	private Logger logger;
	
	public Papyrus2OmlConverter(File inputFolder, File inputFile, OmlCatalog catalog, OmlWriter writer, Logger logger) {
		this.inputFolder = inputFolder;
		this.inputFile = inputFile;
		this.catalog = catalog;
		this.writer = writer;
		this.logger = logger;
	}

	public List<Resource> convert() throws Exception {
		// load the input file
		// ...
					
		// derive an ontology Iri based on the input file
		String relativePath = inputFolder.toPath().normalize().relativize(inputFile.toPath()).toString();
		final String ontologyIri = "http://"+relativePath.substring(0, relativePath.indexOf('.'));

		// create the ontology resource
		final URI ontologyUri = URI.createURI(catalog.resolveURI(ontologyIri)+"."+OML_EXTENSION);
		logger.info("Creating: "+ontologyUri);

		// populate the ontology source
		Ontology ontology = writer.createDescription(ontologyUri, ontologyIri, SeparatorKind.HASH, inputFile.getName());
	
		// return the created ontology resources
		return Collections.singletonList(ontology.eResource());
	}
}
