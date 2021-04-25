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
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.io.CharStreams;

import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.dsl.OmlStandaloneSetup;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;
import io.opencaesar.oml.util.OmlXMIResourceFactory;
import io.opencaesar.papyrus2oml.util.OMLUtil;

public class Papyrus2OmlApp {

	static final List<String> INPUT_EXTENSIONS = Arrays.asList(new String[] {"uml"});

	@Parameter(
			names= {"--input-model-path","-i"}, 
			description="Path to the input Papyrus UML2 model file (Required)",
			validateWith=InputFilePath.class, 
			required=true, 
			order=1
	)
	private String inputModelPath = null;

	@Parameter(
		names= {"--output-catalog-path", "-o"}, 
		description="Path to the output OML catalog file (Required)", 
		validateWith=CatalogPath.class, 
		required=true, 
		order=2
	)
	private String outputCatalogPath;
		
	@Parameter(
			names= {"--ignored-iri-prefix","-p"}, 
			description="Prefixes of IRIs to ignore converting (Optional)",
			required=false, 
			order=3
	)
	private List<String> ignoredIriPrefixes = null;
	
	
	@Parameter(
			names= {"--conversion-type","-c"}, 
			description="Conversion type: uml, dsl, or uml_dsl (Optional)",
			converter = TypeConverter.class,
			required=false, 
			order=4
	)
	private ConversionType conversionType = ConversionType.uml;
	
	@Parameter(
		names= {"--debug", "-d"}, 
		description="Shows debug logging statements", 
		order=5
	)
	private boolean debug;

	@Parameter(
		names= {"--help","-h"}, 
		description="Displays summary of options", 
		help=true, 
		order=6) 
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
		LOGGER.info("Input Model Path= " + inputModelPath);
		LOGGER.info("Output Catalog Path= " + outputCatalogPath);
		
		// create the input model file
		final File inputModelFile = new File(inputModelPath);

		// load the Oml language
		OmlStandaloneSetup.doSetup();
		OmlXMIResourceFactory.register();
		
		// load the Oml catalog
		final URL catalogURL = new File(outputCatalogPath).toURI().toURL();
		final OmlCatalog catalog = OmlCatalog.create(catalogURL);
		
		// create the Oml resource set
		final XtextResourceSet omlResourceSet = new XtextResourceSet();
		final List<Resource> omlResources = new ArrayList<>();

		// create the Oml writer
		final OmlWriter writer = new OmlWriter(omlResourceSet);
		writer.start();
				
		// Convert the input model to OML resources
		Papyrus2OmlConverter converter = new Papyrus2OmlConverter(inputModelFile, ignoredIriPrefixes, catalog, writer, omlResourceSet,conversionType, LOGGER);
		omlResources.addAll(converter.convert());

		// finish the Oml writer
		writer.finish();
		
		// save the Oml resources
		for (Resource resource : omlResources) {
			LOGGER.info("Saving: "+resource.getURI());
			Ontology ontology = OmlRead.getOntology(resource);
			if (!OMLUtil.shouldIgnoreIri(ignoredIriPrefixes, ontology.getIri())) {
				resource.save(Collections.EMPTY_MAP);
			}
		}

		LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}

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

	public static class InputFilePath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File file = new File(value);
			final String fName = file.getName();
			final String fExt = fName.substring(fName.lastIndexOf(".") + 1);
			if (!file.exists() || !fExt.equals("uml")) {
				throw new ParameterException("Argument " + value + " should be a valid Papyrus UML2 file path");
			}
	  	}
	}

	public static class CatalogPath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File file = new File(value);
			if (!file.getName().endsWith("catalog.xml")) {
				throw new ParameterException("Argument " + value + " should be a valid OML catalog path");
			}
		}
		
	}

	public static class TypeConverter implements IStringConverter<ConversionType> {
		@Override
		public ConversionType convert(String value) {
			return ConversionType.valueOf(value);
		}
		
	}

}
