package io.opencaesar.papyrus2oml.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
<<<<<<< HEAD
import org.eclipse.uml2.uml.Element;
=======
import org.eclipse.uml2.uml.NamedElement;
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;

import io.opencaesar.oml.Description;
<<<<<<< HEAD
import io.opencaesar.oml.DescriptionBundle;
=======
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
import io.opencaesar.oml.Literal;
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
<<<<<<< HEAD
		public Map<Element, io.opencaesar.oml.IdentifiedElement> umlToOml = new HashMap<>();
		private Vocabulary umlVoc;
		public DescriptionBundle descriptionBundle;
=======
		public Map<NamedElement, io.opencaesar.oml.IdentifiedElement> umlToOml = new HashMap<>();
		private Vocabulary umlVoc;
>>>>>>> fb0272b46f891d370a0345e19bd8ad35bf7f1965
		
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
		
		public boolean shouldFilterFeature(EStructuralFeature feature) {
			return feature.isDerived() || feature.getName().startsWith("base_");
		}
		
		public boolean shouldFilterFeature(Property prop) {
			return prop.getName().startsWith("base_");
		}
		
		public Literal getLiteralValue(Description description, Object val) {
			if (val instanceof Integer) {
				return writer.createIntegerLiteral(description, (int)val);
			} else if (val instanceof Double) {
				return writer.createDoubleLiteral(description, (double)val);
			} else if (val instanceof Boolean) {
				return writer.createBooleanLiteral(description, (boolean)val);
			} else if (val instanceof EEnumLiteral) {
				return writer.createQuotedLiteral(description, ((EEnumLiteral)val).getLiteral(),null,null);
			} else if (val instanceof Enumerator ) {
				return writer.createQuotedLiteral(description, ((Enumerator)val).getLiteral(),null,null);
			}
			return writer.createQuotedLiteral(description, (String)val,null,null);
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