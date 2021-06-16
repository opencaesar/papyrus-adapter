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

import java.util.List;

import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionExtension;
import io.opencaesar.oml.DescriptionUsage;
import io.opencaesar.oml.Import;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationPredicate;
import io.opencaesar.oml.Rule;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.papyrus2oml.util.ResourceConverter.ConversionContext;

public class OMLUtil {

	static public Relation getSourceRelation(RelationEntity entity, ConversionContext context) {
		Rule rule = (Rule) context.getUmlOmlElementByName(entity.getName()+"_Rule");
		return (rule != null) ? ((RelationPredicate)rule.getConsequent().get(0)).getRelation() : null;
	}
		
	static public Relation getTargetRelation(RelationEntity entity, ConversionContext context) {
		Rule rule = (Rule) context.getUmlOmlElementByName(entity.getName()+"_Rule");
		return (rule != null) ? ((RelationPredicate)rule.getConsequent().get(1)).getRelation() : null;
	}

	static public boolean shouldIgnoreIri(List<String> ignoredIriPrefixes, String iri) {
		if (ignoredIriPrefixes!=null) {
			for (String prefix : ignoredIriPrefixes) {
				if (iri.startsWith(prefix)) {
					return true;
				}
			}
		}
		return false;
	}

	static public DescriptionUsage addUsesIfNeeded(Description description, String iri, OmlBuilder builder) {
		for (Import i : description.getOwnedImports()) {
			if (i instanceof DescriptionUsage && i.getUri().equals(iri)) {
				return (DescriptionUsage) i;
			}
		}
		return builder.addDescriptionUsage(description, iri, null);
	}

	static public DescriptionExtension addExtendsIfNeeded(Description description, String iri, OmlBuilder builder) {
		if (description.getIri().equals(iri)) {
			return null;
		}
		for (Import i : description.getOwnedImports()) {
			if (i instanceof DescriptionExtension && i.getUri().equals(iri)) {
				return (DescriptionExtension) i;
			}
		}
		return builder.addDescriptionExtension(description, iri, null);
	}

}
