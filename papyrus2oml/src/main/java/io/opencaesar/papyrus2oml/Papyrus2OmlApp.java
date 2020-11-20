package io.opencaesar.papyrus2oml;

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
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.io.CharStreams;

import io.opencaesar.oml.dsl.OmlStandaloneSetup;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;

public class Papyrus2OmlApp {

	static final List<String> INPUT_EXTENSIONS = Arrays.asList(new String[] {"uml"});

	@Parameter(
		names= {"--input-folder-path","-i"}, 
		description="Path to the input folder (Required)",
		validateWith=InputFolderPath.class, 
		required=true, 
		order=1
	)
	private String inputFolderPath = null;

	@Parameter(
			names= {"--input-model-path","-r"}, 
			description="Path to the root UML2 model file (Required)",
			validateWith=InputFilePath.class, 
			required=true, 
			order=2
	)
	private String inputModelPath = null;

	@Parameter(
		names= {"--output-catalog-path", "-o"}, 
		description="Path to the output OML catalog file (Required)", 
		validateWith=CatalogPath.class, 
		required=true, 
		order=3
	)
	private String outputCatalogPath;
		
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
		order=5) 
	private boolean help;

	private Logger LOGGER = LogManager.getLogger(Papyrus2OmlApp.class);

	/*
	 * Main method
	 */
	public static void main(String ... args) throws Exception {
		final Papyrus2OmlApp app = new Papyrus2OmlApp();
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
		if (app.inputFolderPath.endsWith(File.separator)) {
			app.inputFolderPath = app.inputFolderPath.substring(0, app.inputFolderPath.length()-1);
		}
		app.run();
	}

	/*
	 * Run method
	 */
	private void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                      Papyrus to Oml "+getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info("Input Folder Path= " + inputFolderPath);
		LOGGER.info("Output Catalog Path= " + outputCatalogPath);
		
		// load the Oml language
		OmlStandaloneSetup.doSetup();
		
		// load the Oml catalog
		final URL catalogURL = new File(outputCatalogPath).toURI().toURL();
		final OmlCatalog catalog = OmlCatalog.create(catalogURL);
		
		// create the Oml resource set
		final XtextResourceSet omlResourceSet = new XtextResourceSet();
		final List<Resource> omlResources = new ArrayList<>();

		// create the Oml writer
		final OmlWriter writer = new OmlWriter(omlResourceSet);
		writer.start();
		
		// create the input folder and files
		final File inputFolder = new File(inputFolderPath);
		final File inputModelFile = new File(inputModelPath);
		
		// Convert the input model to OML resources
		Papyrus2OmlConverter converter = new Papyrus2OmlConverter(inputFolder, inputModelFile, omlResourceSet, catalog, writer, LOGGER);
		omlResources.addAll(converter.convert());

		// finish the Oml writer
		writer.finish();
		
		// save the Oml resources
		for (Resource outputResource : omlResources) {
			LOGGER.info("Saving: "+outputResource.getURI());
			outputResource.save(Collections.EMPTY_MAP);
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

	static public class InputFolderPath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File directory = new File(value);
			if (!directory.isDirectory()) {
				throw new ParameterException("Argument " + value + " should be a valid folder path");
			}
	  	}
	}

	static public class InputFilePath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File file = new File(value);
			if (!file.exists()) {
				throw new ParameterException("Argument " + value + " should be a valid Papyrus UML2 file path");
			}
	  	}
	}

	static public class CatalogPath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File file = new File(value);
			if (!file.getName().endsWith("catalog.xml")) {
				throw new ParameterException("Argument " + value + " should be a valid OML catalog path");
			}
		}
		
	}

}
