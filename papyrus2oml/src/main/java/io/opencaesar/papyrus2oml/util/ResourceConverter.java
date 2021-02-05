package io.opencaesar.papyrus2oml.util;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlWriter;

public abstract class ResourceConverter {

	protected OmlCatalog catalog;
	protected OmlWriter writer;
	protected Logger logger;

	public ResourceConverter(OmlCatalog catalog, OmlWriter writer, Logger logger) {
		this.catalog = catalog;
		this.writer = writer;
		this.logger = logger;
	}
	
	public abstract void convertEObject(EObject eObject) throws IOException;

	public abstract boolean shouldBeIgnored(EObject eObject);
	
	public abstract void finish();

}