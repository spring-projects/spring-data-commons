/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.aot.generate;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.Modifier;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 */
class AotRepositoryImplementationMetadata {

	private ClassName className;
	private Map<String, FieldSpec> fields = new HashMap<>();

	public AotRepositoryImplementationMetadata(ClassName className) {
		this.className = className;
	}

	@Nullable
	public String fieldNameOf(Class<?> type) {

		TypeName lookup = TypeName.get(type).withoutAnnotations();
		for (Entry<String, FieldSpec> field : fields.entrySet()) {
			if (field.getValue().type.withoutAnnotations().equals(lookup)) {
				return field.getKey();
			}
		}

		return null;
	}

	public ClassName getTargetTypeName() {
		return className;
	}

	public String getTargetTypeSimpleName() {
		return className.simpleName();
	}

	public String getTargetTypePackageName() {
		return className.packageName();
	}

	public boolean hasField(String fieldName) {
		return fields.containsKey(fieldName);
	}

	public void addField(String fieldName, TypeName type, Modifier... modifiers) {
		fields.put(fieldName, FieldSpec.builder(type, fieldName, modifiers).build());
	}

	public void addField(FieldSpec fieldSpec) {
		fields.put(fieldSpec.name, fieldSpec);
	}

	Map<String, FieldSpec> getFields() {
		return fields;
	}
}
