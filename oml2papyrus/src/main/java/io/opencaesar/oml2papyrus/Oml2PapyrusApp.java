package io.opencaesar.oml2papyrus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.io.CharStreams;

import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.VocabularyBundle;
import io.opencaesar.oml.dsl.OmlStandaloneSetup;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlXMIResourceFactory;

public class Oml2PapyrusApp {

	@Parameter(
			names= {"--input-ontology-path", "-i"}, 
			description="Path to the input OML ontology (Required)", 
			required=true, 
			order=2
	)
	private String inputOntologyPath;

	@Parameter(
			names= {"--input-profile-path", "-p"}, 
			description="Path to the input UML profile (Optional)", 
			validateWith=InputProfilePath.class, 
			required=false, 
			order=2
	)
	private String inputProfilePath;

	@Parameter(
		names= {"--output-folder-path","-o"}, 
		description="Path to the output Papyrus folder (Required)",
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
	
	@Parameter(
		names= {"--forceReifiedLinks","-f"}, 
		description="Force link to be conveted to reified realtions", 
		order=6
	) 
	private boolean forceReifiedLinks;

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
		LOGGER.info("Input Ontology Path= " + inputOntologyPath);
		LOGGER.info("Input Profile Path= " + inputProfilePath);
		LOGGER.info("Output Folder Path= " + outputFolderPath);
		
		// load the Oml language
		OmlStandaloneSetup.doSetup();
		OmlXMIResourceFactory.register();
				
		// find the root ontology given its URI
		final URI ontologyUri = URI.createFileURI(inputOntologyPath);
		
		// create the Oml resource set
		final XtextResourceSet omlResourceSet = new XtextResourceSet();

		// load the root ontology
		final Resource ontologyResource = omlResourceSet.getResource(ontologyUri, true); 
		final Ontology rootOntology = OmlRead.getOntology(ontologyResource);

		// Create the papyrus resource set
		final ResourceSet papyrusResourceSet = new ResourceSetImpl();
		UMLResourcesUtil.init(papyrusResourceSet);
		Resource papyrusResource = null;
		
		// Output folder
		File outputFolder = new File(outputFolderPath);

		// Convert the input ontology to Papyrus resource
		if (rootOntology instanceof VocabularyBundle) {
			papyrusResource = new VocabularyBundleToProfile((VocabularyBundle)rootOntology, outputFolder, papyrusResourceSet, LOGGER).convert();
		} else if (rootOntology instanceof DescriptionBundle) {
			if (inputProfilePath == null) {
				throw new ParameterException("Input profile path is not specified");
			}
			URI profileUri = URI.createFileURI(inputProfilePath);
			Resource profileResource = papyrusResourceSet.getResource(profileUri, true);
			Profile profile = (Profile) profileResource.getContents().get(0);
			papyrusResource = new DescriptionBundleToModel((DescriptionBundle)rootOntology, profile, outputFolder,forceReifiedLinks, papyrusResourceSet, LOGGER).convert();
		}
				
		// save the Papyrus resources
		if (papyrusResource != null) {
			LOGGER.info("Saving: "+papyrusResource.getURI());
			papyrusResource.save(Collections.EMPTY_MAP);
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
				throw new ParameterException(value + " should be a valid OML catalog path");
			}
		}
		
	}

	static public class InputProfilePath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File file = new File(value);
			if (!file.getName().endsWith("profile.uml") || !file.exists()) {
				throw new ParameterException(value + " should be a valid UML profile path");
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
					throw new ParameterException(value + " should be a valid folder path");
				}
			}
	  	}
	}
}
