package io.opencaesar.oml2papyrus.util;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

public class StereotypeUtils {

	static public Model getUML2Model() {
		ResourceSet rSet = new ResourceSetImpl();
		UMLResourcesUtil.init(rSet);
		Resource resource = rSet.getResource(URI.createURI(UMLResource.UML_METAMODEL_URI), true);
		try {
			resource.load(null);
		} catch (IOException e) {
			// TODO LOG
			return null;
		}
		Model umlMetamodel = (Model) EcoreUtil.getObjectByType(resource.getContents(), Literals.PACKAGE);
		return umlMetamodel;
	}

	static public Stereotype createStereotype(Profile profile, String name, boolean isAbstract, Model umlModel,
			String... metaClassNames) {
		final Stereotype createdST = createStereotype(profile, name, isAbstract);
		if (metaClassNames != null && metaClassNames.length > 0) {
			for (String metaClassName : metaClassNames) {
				extendMetaclass(umlModel, profile, metaClassName, createdST);
			}
		}
		return createdST;

	}

	static public Stereotype createStereotype(Profile profile, String name, boolean isAbstract) {
		final Stereotype createdST = profile.createOwnedStereotype("TestStereoType", isAbstract);
		return createdST;
	}

	private static void extendMetaclass(final Model umlMetamodel, final Profile profile, final String name,
			final Stereotype stereotype) {
		stereotype.createExtension(referenceMetaclass(umlMetamodel, profile, name), true);
	}

	private static org.eclipse.uml2.uml.Class referenceMetaclass(final Model umlMetamodel, final Profile profile,
			final String name) {
		final org.eclipse.uml2.uml.Class metaclass = (org.eclipse.uml2.uml.Class) umlMetamodel.getOwnedType(name);
		if (!profile.getReferencedMetaclasses().contains(metaclass)) {
			profile.createMetaclassReference(metaclass);
		}
		return metaclass;
	}

}
