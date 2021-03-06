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
package io.opencaesar.oml2papyrus.util;

import java.util.Collection;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageImport;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

public class ProfileUtils {

	public static Model getUMLMetamodel(ResourceSet resourceSet) {
		Resource resource = resourceSet.getResource(URI.createURI(UMLResource.UML_METAMODEL_URI), true);
		if (resource != null) {
			return (Model) EcoreUtil.getObjectByType(resource.getContents(), UMLPackage.Literals.MODEL);
		}
		return null;
	}
	
	public static Model getPrimitiveTypesLibrary(ResourceSet resourceSet) {
		Resource resource = resourceSet.getResource(URI.createURI(UMLResource.UML_PRIMITIVE_TYPES_LIBRARY_URI), true);
		if (resource != null) {
			return (Model) EcoreUtil.getObjectByType(resource.getContents(), UMLPackage.Literals.MODEL);
		}
		return null;
	}

	public static Profile createProfile(ResourceSet resourceSet, URI resourceUri, String profileName, String profileURI) {
		Resource resource = resourceSet.createResource(resourceUri);
		if (resource == null)
			return null;
		
		Profile profile = UMLFactory.eINSTANCE.createProfile();
		profile.setName(profileName);
		profile.setURI(profileURI);
		resource.getContents().add(profile);
		
		// create a PackageImport for UML and treat it as a metamodel reference
		profile.createMetamodelReference(getUMLMetamodel(resourceSet));
		
		// create a regular PackageImport for the PrimitiveTypes
		PackageImport primitiveTypesLibraryImport = UMLFactory.eINSTANCE.createPackageImport();
		primitiveTypesLibraryImport.setImportedPackage(getPrimitiveTypesLibrary(resourceSet));
		profile.getPackageImports().add(primitiveTypesLibraryImport);
		
		return profile;
	}

	public static Package createPackage(Package profile, String packageName, String packageURI) {
		Package package_ = UMLFactory.eINSTANCE.createPackage();
		package_.setName(packageName);
		package_.setURI(packageURI);
		profile.getPackagedElements().add(package_);
		return package_;
	}
	
	public static Class createClass(Package package_, String className, boolean isAbstract) {
		return package_.createOwnedClass(className, isAbstract);
	}

	public static Stereotype createStereotype(Package package_, String name, boolean isAbstract, Collection<String> metaClassNames) {
		final Stereotype createdST = package_.createOwnedStereotype(name, isAbstract);
		if (metaClassNames != null && metaClassNames.size() > 0) {
			Model umlMetamodel = getUMLMetamodel(package_.eResource().getResourceSet());
			for (String metaClassName : metaClassNames) {
				extendMetaclass(umlMetamodel, getProfile(package_), metaClassName, createdST);
			}
		}
		return createdST;

	}
	
	private static Profile getProfile(Package pkg) {
		if (pkg == null || pkg instanceof Profile) {
			return (Profile)pkg;
		}
		return getProfile(pkg.getNestingPackage());
	}

	private static void extendMetaclass(final Model umlMetamodel, final Profile profile, final String name, final Stereotype stereotype) {
		stereotype.createExtension(referenceMetaclass(umlMetamodel, profile, name), false);
	}

	private static org.eclipse.uml2.uml.Class referenceMetaclass(final Model umlMetamodel, final Profile profile, final String name) {
		return (org.eclipse.uml2.uml.Class) umlMetamodel.getOwnedType(name);
	}

}
