package io.opencaesar.oml2papyrus;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.resource.UMLResource;

import io.opencaesar.oml.Annotation;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.BooleanLiteral;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.QuotedLiteral;
import io.opencaesar.oml.Type;
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
	
	public Oml2PapyrusConverter(Ontology rootOntology, OmlCatalog catalog, File papyrusFolder, ResourceSet papyrusResourceSet, Logger logger) {
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
		URI profileUri = URI.createFileURI(papyrusFolder.getAbsolutePath()+File.separator+rootOntology.getPrefix()+'.'+UMLResource.PROFILE_FILE_EXTENSION);
		Profile profile = ProfileUtils.createProfile(umlResourceSet, profileUri, rootOntology.getPrefix());
		
		// get all imported vocabularies
		List<Vocabulary> allVocabularies = OmlRead.getAllImportedOntologies(rootOntology).stream().
				filter(o -> o instanceof Vocabulary).
				map(o -> (Vocabulary)o).
				collect(Collectors.toList());
		
		// create profile content
		for (Vocabulary vocabulary : allVocabularies) {
			// create package
			org.eclipse.uml2.uml.Package package_ = ProfileUtils.createPackage(profile, vocabulary.getPrefix());
			
			// get all vocabulary types
			List<Type> allTypes = vocabulary.getOwnedStatements().stream().
					filter(s -> s instanceof Type).
					map(s -> (Type)s).
					collect(Collectors.toList());
			
			// create package contents
			for (Type type : allTypes) {
				if (type instanceof Entity) {
					List<Annotation> annotations = OmlSearch.findAnnotations(type);
					
					// if it is a sterotype
					boolean isStereotype = annotations.stream().
							filter(a -> IS_STEREOTYPE.equals(OmlRead.getIri(a.getProperty()))).
							count() > 0;
					
					// if it is a sterotype of some metaclasses
					List<String> isStereotypeOfs = annotations.stream().
							filter(a -> IS_STEREOTYPE_OF.equals(OmlRead.getIri(a.getProperty()))).
							map(a -> a.getValue()).
							filter(v -> v instanceof QuotedLiteral).
							map(v -> ((QuotedLiteral)v).getValue()).
							map(v -> v.substring(v.indexOf(':')+1)).
							collect(Collectors.toList());

					Class class_ = null;
					if (isStereotype || !isStereotypeOfs.isEmpty()) {
						// create steroetype
						class_ = ProfileUtils.createStereotype(package_, type.getName(), type instanceof Aspect, Collections.emptyList());
					} else {
						// create class
						class_ = ProfileUtils.createClass(package_, type.getName(), type instanceof Aspect);
					}
				}
			}
			
		}

		// return the created Papyrus resources
		return profile.eResource();
	}
	
}
