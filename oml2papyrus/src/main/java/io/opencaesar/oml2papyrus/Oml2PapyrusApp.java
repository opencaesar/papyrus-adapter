package io.opencaesar.oml2papyrus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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

	static final List<String> INPUT_EXTENSIONS = Arrays.asList(new String[] {"txt", "pdf"});

	@Parameter(
			names= {"--input-catalog-path", "-i"}, 
			description="Path to the input OML catalog file (Required)", 
			validateWith=InputCatalogPath.class, 
			required=true, 
			order=1
	)
	private String inputCatalogPath;

	@Parameter(
			names= {"--input-ontology-path", "-r"}, 
			description="Path to the input OML ontology (Required)", 
			validateWith=InputFilePath.class, 
			required=true, 
			order=2
	)
	private String inputOntologyPath;

	@Parameter(
		names= {"--input-folder-path","-i"}, 
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

	private Logger LOGGER = LogManager.getLogger(Oml2PapyrusApp.class);

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
		LOGGER.info("Input Ontology Path= " + inputOntologyPath);
		LOGGER.info("Output Folder Path= " + outputFolderPath);
		
		// load the Oml language
		OmlStandaloneSetup.doSetup();
		OmlXMIResourceFactory.register();
		
		// create the Oml resource set
		final XtextResourceSet omlResourceSet = new XtextResourceSet();

		// load the Oml catalog (to load ontologies by IRI)
		final URL catalogURL = new File(inputCatalogPath).toURI().toURL();
		final OmlCatalog catalog = OmlCatalog.create(catalogURL);
		
		// load the root ontology from its URI
		final URI ontologyUri = URI.createFileURI(inputOntologyPath);
		final Resource ontologyResource = omlResourceSet.getResource(ontologyUri, true); 
		final Ontology rootOntology = OmlRead.getOntology(ontologyResource);

		// Create the papyrus resource set
		ResourceSet papyrusResourceSet = new ResourceSetImpl();
		
		// Papyrus folder
		File papyrusFolder = new File(outputFolderPath);

		// Convert the input ontology to Papyrus resources
		Oml2PapyrusConverter converter = new Oml2PapyrusConverter(rootOntology, catalog, papyrusFolder, papyrusResourceSet, LOGGER);
		List<Resource> papyrusResources = new ArrayList<>();
		papyrusResources.addAll(converter.convert());
				
		// save the Papyrus resources
		for (Resource resource : papyrusResources) {
			LOGGER.info("Saving: "+resource.getURI());
			resource.save(Collections.EMPTY_MAP);
		}

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

	static public class InputFilePath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File file = new File(value);
			if (!file.exists()) {
				throw new ParameterException("Parameter " + name + " should be a valid file path");
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
