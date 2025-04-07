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

import java.util.List;

import org.springframework.data.repository.aot.generate.json.JSONArray;
import org.springframework.data.repository.aot.generate.json.JSONException;
import org.springframework.data.repository.aot.generate.json.JSONObject;

/**
 * @author Mark Paluch
 * @since 4.0
 */
record AotRepositoryMetadata(String name, String module,
		org.springframework.data.repository.aot.generate.AotRepositoryMetadata.RepositoryType type,
		List<AotRepositoryMethod> methods) {

	enum RepositoryType {
		IMPERATIVE, REACTIVE
	}

	JSONObject toJson() throws JSONException {

		JSONObject metadata = new JSONObject();
		metadata.put("name", name());
		metadata.put("module", module());
		metadata.put("type", type().name());

		JSONArray methods = new JSONArray();

		for (AotRepositoryMethod method : methods()) {
			methods.put(method.toJson());
		}

		metadata.put("methods", methods);

		return metadata;

	}
}
