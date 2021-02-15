package io.opencaesar.papyrus2oml.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;

import io.opencaesar.oml.Member;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public abstract class ResourceConverter {
	
	public static class ConversionContext {
		public OmlCatalog catalog;
		public OmlWriter writer;
		public Logger logger;
		public List<Runnable> deferred = new ArrayList<>();
		public Package rootPackage;
		public Map<NamedElement, io.opencaesar.oml.IdentifiedElement> umlToOml = new HashMap<>();
		private Vocabulary umlVoc;
		
		public ConversionContext(OmlCatalog cat, OmlWriter writer, Logger logger) {
			this.catalog = cat;
			this.writer = writer;
			this.logger = logger;
		}
		
		public ConversionContext(OmlCatalog cat, OmlWriter writer, ResourceSet rs, Logger logger) {
			this(cat,writer,logger);
			this.catalog = cat;
			this.writer = writer;
			this.logger = logger;
			try {
				final URI umlUri = URI.createURI(catalog.resolveURI(UmlUtils.UML_IRI) + "." + OmlConstants.OML_EXTENSION);
				Resource r = rs.getResource(umlUri, true);
				umlVoc = (Vocabulary) OmlRead.getOntology(r);
				
			} catch (IOException e) {
				logger.error("Failed to load uml voc");
			}
		}
		
		public Member getUmlOmlElementByName(String name) {
			return OmlRead.getMemberByName(umlVoc, name);
		}
		
		
	}

	protected ConversionContext context;
	

	public ResourceConverter(ConversionContext context) {
		this.context = context;
	}
	
	public abstract void convertEObject(EObject eObject) throws IOException;

	public abstract boolean shouldBeIgnored(EObject eObject);
	
	public abstract void finish();

}