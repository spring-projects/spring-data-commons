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

import org.jspecify.annotations.Nullable;

import org.springframework.data.repository.aot.generate.json.JSONException;
import org.springframework.data.repository.aot.generate.json.JSONObject;

/**
 * @author Mark Paluch
 * @since 4.0
 */
record AotRepositoryMethod(String name, String signature, @Nullable QueryMetadata query,
		@Nullable AotFragmentTarget fragment) {

	public JSONObject toJson() throws JSONException {

		JSONObject method = new JSONObject();
		method.put("name", name());
		method.put("signature", signature());

		if (query() != null) {
			method.put("query", query().toJson());
		} else if (fragment() != null) {
			method.put("fragment", fragment().toJson());
		}

		return method;
	}
}
