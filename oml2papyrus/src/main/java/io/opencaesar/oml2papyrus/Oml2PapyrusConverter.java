package io.opencaesar.oml2papyrus;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.resource.UMLResource;

import io.opencaesar.oml.Annotation;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.QuotedLiteral;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml2papyrus.util.ProfileUtils;

public class Oml2PapyrusConverter {

	private static final String IS_STEREOTYPE = "http://www.eclipse.org/uml2/5.0.0/UML-Annotations#isStereotype";
	private static final String IS_STEREOTYPE_OF = "http://www.eclipse.org/uml2/5.0.0/UML-Annotations#isStereotypeOf";

	private Ontology rootOntology;
	private OmlCatalog catalog;
	private File papyrusFolder;
	private ResourceSet papyrusResourceSet;
	private Logger logger;

	public Oml2PapyrusConverter(Ontology rootOntology, OmlCatalog catalog, File papyrusFolder,
			ResourceSet papyrusResourceSet, Logger logger) {
		this.rootOntology = rootOntology;
		this.catalog = catalog;
		this.papyrusFolder = papyrusFolder;
		this.papyrusResourceSet = papyrusResourceSet;
		this.logger = logger;
	}

	public Resource convert() throws Exception {
		// Create the UML resource set
		ResourceSet umlResourceSet = new ResourceSetImpl();
		ProfileUtils.initResourceSet(umlResourceSet);

		// Create profile
		URI profileUri = URI.createFileURI(papyrusFolder.getAbsolutePath() + File.separator + rootOntology.getPrefix()
				+ '.' + UMLResource.PROFILE_FILE_EXTENSION);
		Profile profile = ProfileUtils.createProfile(umlResourceSet, profileUri, rootOntology.getPrefix());

		Collection<StereoTypesInfo> entryPoints = getEntryPoints(rootOntology);
		for (StereoTypesInfo entry : entryPoints) {
			System.out.println(entry);
		}

		Map<Vocabulary, Package> voc2Package = new HashMap<>();

		for (StereoTypesInfo entryPoint : entryPoints) {
			// create steroetype
			Vocabulary voc = entryPoint.vocabulary;
			Package pkg = voc2Package.get(voc);
			if (pkg == null) {
				pkg = ProfileUtils.createPackage(profile, voc.getPrefix());
				voc2Package.put(voc, pkg);
			}
			ProfileUtils.createStereotype(pkg, entryPoint.entity.getName(), entryPoint.entity instanceof Aspect,
					entryPoint.metaClasses);
		}

		/*
		 * // create class class_ = ProfileUtils.createClass(package_, type.getName(),
		 * type instanceof Aspect); } // return the created Papyrus resources
		 */

		return profile.eResource();
	}

	private static class StereoTypesInfo {
		public Vocabulary vocabulary;
		public Entity entity;
		public Annotation annotation;
		public Set<String> metaClasses = new HashSet<>();

		public StereoTypesInfo(Vocabulary voc, Entity entity, Annotation annot) {
			this.vocabulary = voc;
			this.entity = entity;
			this.annotation = annot;
		}

		public boolean isSteroTypeOf() {
			return metaClasses.size() > 0;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(vocabulary.getIri() + " -> " + entity.getName());
			if (metaClasses.size() > 0) {
				builder.append("\n StereoTypeOf : ");
				metaClasses.forEach(a -> builder.append(a));
			}
			return builder.toString();
		}

		public void addMetaClass(String metaClassName) {
			metaClasses.add(metaClassName);
		}
	}

	/**
	 * return all annotated Entities as Stereo types
	 * 
	 * @param rootOntology the root Ontology
	 * @return all entry points represented as StereoType Entry
	 */
	private Collection<StereoTypesInfo> getEntryPoints(Ontology rootOntology) {
		List<Vocabulary> directVoc = OmlRead.getAllImportedOntologies(rootOntology).stream()
				.filter(ontology -> ontology instanceof Vocabulary).map(ontology -> (Vocabulary) ontology)
				.collect(Collectors.toList());
		Map<Entity, StereoTypesInfo> entityToInfo = new LinkedHashMap<>();

		// now we have all direct voc let's find the stereo types CI
		for (Vocabulary voc : directVoc) {
			// get all voc entities
			List<Entity> entities = voc.getOwnedStatements().stream().filter(statement -> {
				return statement instanceof Entity;
			}).map(statement -> (Entity) statement).collect(Collectors.toList());
			// not all entities will be sterotyped we need to find the sterotypes ones
			for (Entity entity : entities) {
				// find it is annotations
				OmlSearch.findAnnotations(entity).forEach(annotation -> {
					if (IS_STEREOTYPE.equals(OmlRead.getIri(annotation.getProperty()))) {
						entityToInfo.put(entity, new StereoTypesInfo(voc, entity, annotation));
					} else if (IS_STEREOTYPE_OF.equals(OmlRead.getIri(annotation.getProperty()))) {
						Literal value = annotation.getValue();
						if (value instanceof QuotedLiteral) {
							QuotedLiteral qLiteral = (QuotedLiteral) value;
							String sValue = qLiteral.getValue();
							StereoTypesInfo info = entityToInfo.get(entity);
							if (info == null) {
								info = new StereoTypesInfo(voc, entity, annotation);
								entityToInfo.put(entity, info);
							}
							info.addMetaClass(sValue.substring(sValue.indexOf(':') + 1));
						}
					}
				});
			}
		}
		return entityToInfo.values();
	}

}
