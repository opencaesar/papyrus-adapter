package io.opencaesar.oml2papyrus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.io.CharStreams;

import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.dsl.OmlStandaloneSetup;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlXMIResourceFactory;

public class Oml2PapyrusApp {

	private static final String OML_EXTENSION = "oml";
	private static final String OML_XMI_EXTENSION = "omlxmi";

	@Parameter(
			names= {"--input-catalog-path", "-i"}, 
			description="Path to the input OML catalog file (Required)", 
			validateWith=InputCatalogPath.class, 
			required=true, 
			order=1
	)
	private String inputCatalogPath;

	@Parameter(
			names= {"--input-ontology-iri", "-r"}, 
			description="IRI of the input OML ontology (Required)", 
			required=true, 
			order=2
	)
	private String inputOntologyIri;

	@Parameter(
		names= {"--output-folder-path","-o"}, 
		description="Path to the output papyrus folder (Required)",
		validateWith=OutputFolderPath.class, 
		required=true, 
		order=3
	)
	private String outputFolderPath = null;
		
	@Parameter(
		names= {"--debug", "-d"}, 
		description="Shows debug logging statements", 
		order=4
	)
	private boolean debug;

	@Parameter(
		names= {"--help","-h"}, 
		description="Displays summary of options", 
		help=true, 
		order=5
	) 
	private boolean help;

    private final static Logger LOGGER = Logger.getLogger(Oml2PapyrusApp.class);

    static {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
    }

	/*
	 * Main method
	 */
	public static void main(String ... args) throws Exception {
		final Oml2PapyrusApp app = new Oml2PapyrusApp();
		final JCommander builder = JCommander.newBuilder().addObject(app).build();
		builder.parse(args);
		if (app.help) {
			builder.usage();
			return;
		}
		if (app.debug) {
			final Appender appender = LogManager.getRootLogger().getAppender("stdout");
			((AppenderSkeleton)appender).setThreshold(Level.DEBUG);
		}
		if (app.outputFolderPath.endsWith(File.separator)) {
			app.outputFolderPath = app.outputFolderPath.substring(0, app.outputFolderPath.length()-1);
		}
		app.run();
	}

	/*
	 * Run method
	 */
	private void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                      Oml to Papyrus "+getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info("Input Catalog Path= " + inputCatalogPath);
		LOGGER.info("Input Ontology Iri= " + inputOntologyIri);
		LOGGER.info("Output Folder Path= " + outputFolderPath);
		
		// load the Oml language
		OmlStandaloneSetup.doSetup();
		OmlXMIResourceFactory.register();
		
		// create the Oml resource set
		final XtextResourceSet omlResourceSet = new XtextResourceSet();

		// load the Oml catalog (to load ontologies by IRI)
		final URL catalogURL = new File(inputCatalogPath).toURI().toURL();
		final OmlCatalog catalog = OmlCatalog.create(catalogURL);
		
		// find the root ontology given its URI
		final URI baseIri = URI.createURI(catalog.resolveURI(inputOntologyIri));
		final URI ontologyUri;
		if (new File(baseIri.toFileString()+"."+OML_EXTENSION).exists()) {
			ontologyUri = URI.createURI(baseIri+"."+OML_EXTENSION);
		} else if (new File(baseIri.toFileString()+"."+OML_XMI_EXTENSION).exists()) {
			ontologyUri = URI.createURI(baseIri+"."+OML_XMI_EXTENSION);
		} else {
			throw new RuntimeException("Ontology with iri '"+"' cannot be found in the catalog");
		}
		
		// load the root ontology
		final Resource ontologyResource = omlResourceSet.getResource(ontologyUri, true); 
		final Ontology rootOntology = OmlRead.getOntology(ontologyResource);

		// Create the papyrus resource set
		ResourceSet papyrusResourceSet = new ResourceSetImpl();
		
		// Papyrus folder
		File papyrusFolder = new File(outputFolderPath);

		// Convert the input ontology to Papyrus resource
		Oml2PapyrusConverter converter = new Oml2PapyrusConverter(rootOntology, papyrusFolder, papyrusResourceSet, LOGGER);
		Resource resource = converter.convert();
				
		// save the Papyrus resource
		LOGGER.info("Saving: "+resource.getURI());
		resource.save(Collections.EMPTY_MAP);

		LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}

	// Utility methods
	
	/**
	 * Get application version id from properties file.
	 * @return version string from build.properties or UNKNOWN
	 */
	private String getAppVersion() {
		String version = "UNKNOWN";
		try {
			final InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("version.txt");
			final InputStreamReader reader = new InputStreamReader(input);
			version = CharStreams.toString(reader);
		} catch (IOException e) {
			final String errorMsg = "Could not read version.txt file." + e;
			LOGGER.error(errorMsg, e);
		}
		return version;
	}

	static public class InputCatalogPath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File file = new File(value);
			if (!file.getName().endsWith("catalog.xml")) {
				throw new ParameterException("Parameter " + name + " should be a valid OML catalog path");
			}
		}
		
	}

	static public class OutputFolderPath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File directory = new File(value);
			if (!directory.isDirectory()) {
				directory.mkdirs();
				if (!directory.isDirectory()) {
					throw new ParameterException("Parameter " + name + " should be a valid folder path");
				}
			}
	  	}
	}
}
