package io.opencaesar.papyrus2oml.util;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Profile;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;

public class ProfileConverter extends ResourceConverter {

	protected Profile profile;
	
	public ProfileConverter(Profile profile, OmlCatalog catalog, OmlWriter writer, Logger logger) {
		super(catalog, writer, logger);
		this.profile = profile;
	}
	
	@Override
	public void convertEObject(EObject eObject) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean shouldBeIgnored(EObject eObject) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void finish() {
	}
}