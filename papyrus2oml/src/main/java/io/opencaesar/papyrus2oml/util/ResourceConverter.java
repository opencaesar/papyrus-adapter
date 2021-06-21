/**
 * 
 * Copyright 2021 Modelware Solutions and CAE-LIST.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
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
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.papyrus2oml.ConversionType;

public abstract class ResourceConverter {
	
	public static class ConversionContext {
		public List<String> ignoredIriPrefixes;
		public OmlCatalog catalog;
		public OmlBuilder builder;
		public Logger logger;
		public List<Runnable> deferredRelations = new ArrayList<>();
		public List<Runnable> deferredLinks = new ArrayList<>();
		public Package rootPackage;
		public Map<Element, io.opencaesar.oml.IdentifiedElement> umlToOml = new HashMap<>();
		private Vocabulary umlVoc;
		public DescriptionBundle descriptionBundle;
		public ConversionType conversionType;
		public String postFix = "";
		public boolean DSL = false;

		public ConversionContext(OmlCatalog cat, OmlBuilder builder, ConversionType conversionType, Logger logger) {
			this.catalog = cat;
			this.builder = builder;
			this.logger = logger;
			this.conversionType = conversionType;
		}
		
		public ConversionContext(List<String> ignoredIriPrefixes, OmlCatalog cat, OmlBuilder builder, ResourceSet rs, ConversionType conversionType, Logger logger) {
			this(cat,builder,conversionType,logger);
			this.ignoredIriPrefixes = ignoredIriPrefixes;
			try {
				final URI umlUri = URI.createURI(catalog.resolveURI(UmlUtils.UML_IRI) + "." + OmlConstants.OML_EXTENSION);
				Resource r = rs.getResource(umlUri, true);
				umlVoc = (Vocabulary) OmlRead.getOntology(r);
				
			} catch (IOException e) {
				logger.error("Failed to load uml voc");
			}
		}
		
		public IdentifiedElement getOmlElementForIgnoredElement(Element element, Description description) {
			String targetIri = getIgnoredElementIRI(element, this);
			Member omlElement = OmlRead.getMemberByIri(description.eResource().getResourceSet(), targetIri);
			if(omlElement==null) {
				throw new RuntimeException("Element " + targetIri + " cannot be found");
			}
			return omlElement;
		}
		
		private static String getIgnoredElementIRI(Element element, ConversionContext context) {
			Package pkg = element.getNearestPackage();
			// try load the elements ontology directly from context 
			var bundleResource = context.umlVoc.eResource();
			var ontologyUri = OmlRead.getResolvedUri(bundleResource, URI.createURI(pkg.getURI()));
			Resource ontologyResource = bundleResource.getResourceSet().getResource(ontologyUri, true);
			Ontology ontology = OmlRead.getOntology(ontologyResource);
			return ontology.getNamespace() + UmlUtils.getName(element);
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
				return builder.createIntegerLiteral((int)val);
			} else if (val instanceof Double) {
				return builder.createQuotedLiteral(description, val.toString(), OmlConstants.XSD_NS+"double", null);
			} else if (val instanceof Boolean) {
				return builder.createBooleanLiteral((boolean)val);
			} else if (val instanceof EEnumLiteral) {
				return builder.createQuotedLiteral(null, ((EEnumLiteral)val).getLiteral(),null,null);
			} else if (val instanceof Enumerator ) {
				return builder.createQuotedLiteral(null, ((Enumerator)val).getLiteral(),null,null);
			}
			return builder.createQuotedLiteral(null, (String)val,null,null);
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