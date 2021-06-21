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

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Profile;

import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.papyrus2oml.ConversionType;

public class ProfileConverter extends ResourceConverter {

	protected Profile profile;
	
	public ProfileConverter(Profile profile, OmlCatalog catalog, OmlBuilder builder, ConversionType conversionType, Logger logger) {
		super(new ConversionContext(catalog, builder,conversionType, logger));
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