package io.opencaesar.papyrus2oml.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Stereotype;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.DescriptionExtension;
import io.opencaesar.oml.DescriptionUsage;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Import;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.SourceRelation;
import io.opencaesar.oml.TargetRelation;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class DSLPackageConverter extends ResourceConverter {

	protected Package rootPackage;
	
	private Map<NamedElement, io.opencaesar.oml.IdentifiedElement> umlToOml = new HashMap<>();
	
	private List<Runnable> deferred = new ArrayList<>();

	public DSLPackageConverter(Package rootPackage, OmlCatalog catalog, OmlWriter writer, Logger logger) {
		super(catalog, writer, logger);
		this.rootPackage = rootPackage;
	}
	
	@Override
	public boolean shouldBeIgnored(EObject eObject) {
		return !(eObject instanceof Element);
	}
	
	@Override
	public void finish() {
		deferred.forEach(r -> r.run());
	}

	@Override
	public void convertEObject(EObject eObject) throws IOException {
		if (eObject == rootPackage) {
			createDescriptionBundle(rootPackage);
		} else if (eObject instanceof Package) {
			createDescription((Package)eObject);
		} else if (eObject instanceof PackageableElement) {
			createNamedInstance((PackageableElement)eObject);
		}
	}

	protected void createDescriptionBundle(Package package_) throws IOException {
		final String prefix = package_.getName();
		final String iri = package_.getURI();
		final URI uri = URI.createURI(catalog.resolveURI(iri)+"."+OmlConstants.OML_EXTENSION);
		DescriptionBundle bundle = writer.createDescriptionBundle(uri, iri, SeparatorKind.HASH, prefix);
		umlToOml.put(package_, bundle);
	}

	protected void createNamedInstance(PackageableElement element) throws IOException {
		String name = element.getName();
		if (name == null || name.isEmpty()) {
			// Notice that some relations in UML could be anonymous
			// so we need to still handle them as unreified relations
			return;
		}
		
		Description description = (Description) umlToOml.get(element.getNearestPackage());
		List<Stereotype> stereotypes = element.getAppliedStereotypes();

		ResourceSet rs = description.eResource().getResourceSet();

		List<Member> types = new ArrayList<>();
		for(Stereotype s : stereotypes) {
			Package package_ = s.getNearestPackage();
			Import i = addUsesIfNeeded(description, package_.getURI());
			Member type = (Member) umlToOml.get(s);
			if (type == null) {
				URI uri = OmlRead.getResolvedImportUri(i);
				if (uri == null) {
					throw new RuntimeException("Cannot resolve IRI '"+package_.getURI()+"'");
				}
				Resource r = rs.getResource(uri, true);
				Vocabulary vocabulary = (Vocabulary) OmlRead.getOntology(r);
				if (vocabulary == null) {
					throw new RuntimeException("Cannot load vocabylary '"+package_.getURI()+"'");
				}
				type = OmlRead.getMemberByName(vocabulary, s.getName());
				if (type == null) {
					throw new RuntimeException("Cannot find entity equivalent to '"+s.getQualifiedName()+"'");
				}
				umlToOml.put(s, type);
			}
			types.add(type);
		}
				
		Member type = types.stream().filter(t -> !(t instanceof Aspect)).findFirst().orElse(null);
		
		if (type instanceof Concept) {
			ConceptInstance instance = writer.addConceptInstance(description, element.getName());
			umlToOml.put(element, instance);
			String instanceIri = OmlRead.getIri(instance);
			for (Member t : types) {
				writer.addConceptTypeAssertion(description, instanceIri, OmlRead.getIri(t));
			}
		} else if (type instanceof RelationEntity) {
			deferred.add(new Runnable() {
				@Override
				public void run() {
					RelationEntity entity = (RelationEntity) type;
					
					SourceRelation sourceR = entity.getSourceRelation();
					while (sourceR == null && !entity.getOwnedSpecializations().isEmpty()) {
						RelationEntity superEntity = entity.getOwnedSpecializations().stream()
								.map(s -> s.getSpecializedTerm())
								.filter(t -> t instanceof RelationEntity)
								.map(t -> (RelationEntity)t)
								.findFirst()
								.orElse(null);
						if (superEntity == null) {
							break;
						}
						sourceR = entity.getSourceRelation();
					}
					
					List<String> sources = new ArrayList<>();
					if (sourceR != null) {
						String name = sourceR.getName().split("_")[1];
						EStructuralFeature f = element.eClass().getEStructuralFeature(name);
						Object values = element.eGet(f);
						if (values instanceof Collection<?>) {
							for (Object value : ((Collection<?>)values)) {
								IdentifiedElement e = umlToOml.get(value);
								sources.add(OmlRead.getIri(e));
								addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri());
							}
						} else {
							IdentifiedElement e = umlToOml.get(values);
							sources.add(OmlRead.getIri(e));
							addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri());
						}
					}

					TargetRelation targetR = entity.getTargetRelation();
					while (targetR == null && !entity.getOwnedSpecializations().isEmpty()) {
						RelationEntity superEntity = entity.getOwnedSpecializations().stream()
								.map(s -> s.getSpecializedTerm())
								.filter(t -> t instanceof RelationEntity)
								.map(t -> (RelationEntity)t)
								.findFirst()
								.orElse(null);
						if (superEntity == null) {
							break;
						}
						targetR = entity.getTargetRelation();
					}

					List<String> targets = new ArrayList<>();
					if (targetR != null) {
						String name = targetR.getName().split("_")[1];
						EStructuralFeature f = element.eClass().getEStructuralFeature(name);
						Object values = element.eGet(f);
						if (values instanceof Collection<?>) {
							for (Object value : ((Collection<?>)values)) {
								IdentifiedElement e = umlToOml.get(value);
								targets.add(OmlRead.getIri(e));
								addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri());
							}
						} else {
							IdentifiedElement e = umlToOml.get(values);
							targets.add(OmlRead.getIri(umlToOml.get(values)));
							addExtendsIfNeeded(description, OmlRead.getOntology(e).getIri());
						}
					}
					
					RelationInstance instance = DSLPackageConverter.this.writer.addRelationInstance(description, element.getName(), sources, targets);
					String instanceIri = OmlRead.getIri(instance);
					for (Member t : types) {
						writer.addRelationTypeAssertion(description, instanceIri, OmlRead.getIri(t));
					}
				}
			});
		}
	}
	
	protected void createDescription(Package package_) throws IOException {
		boolean empty = package_.getPackagedElements().stream()
			.filter(e -> !(e instanceof Package))
			.count() == 0;
			
		if (!empty) {
			final String prefix = package_.getName();
			final String iri = package_.getURI();
			final URI uri = URI.createURI(catalog.resolveURI(iri)+"."+OmlConstants.OML_EXTENSION);
			
			Description description = writer.createDescription(uri, iri, SeparatorKind.HASH, prefix);
			umlToOml.put(package_, description);

			writer.addDescriptionUsage(description, OmlConstants.OWL_IRI, null);

			DescriptionBundle bundle = (DescriptionBundle) umlToOml.get(rootPackage);
			writer.addDescriptionBundleInclusion(bundle, iri, null);
		}
	}

	protected DescriptionUsage addUsesIfNeeded(Description description, String iri) {
		for (Import i : description.getOwnedImports()) {
			if (i instanceof DescriptionUsage && i.getUri().equals(iri)) {
				return (DescriptionUsage) i;
			}
		}
		return writer.addDescriptionUsage(description, iri, null);
	}

	protected DescriptionExtension addExtendsIfNeeded(Description description, String iri) {
		for (Import i : description.getOwnedImports()) {
			if (i instanceof DescriptionExtension && i.getUri().equals(iri)) {
				return (DescriptionExtension) i;
			}
		}
		return writer.addDescriptionExtension(description, iri, null);
	}
}