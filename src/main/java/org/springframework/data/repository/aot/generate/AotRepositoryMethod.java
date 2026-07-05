/*
 * Copyright 2025-present the original author or authors.
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

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Value object capturing metadata about a repository method.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public record AotRepositoryMethod(String name, String signature, @Nullable QueryMetadata query,
		@Nullable AotFragmentTarget fragment) {

	/**
	 * Convert this {@link AotRepositoryMethod} to a {@link JSONObject}.
	 *
	 * @return
	 * @throws JSONException
	 */
	public JSONObject toJson() throws JSONException {

		JSONObject method = new JSONObject();
		method.put("name", name());
		method.put("signature", signature());

		if (query() != null) {
			method.put("query", queryMetadataToJson(query()));
		} else if (fragment() != null) {
			method.put("fragment", fragment().toJson());
		}

		return method;
	}

	static JSONObject queryMetadataToJson(QueryMetadata queryMetadata) throws JSONException {

		JSONObject query = new JSONObject();

		for (Map.Entry<String, Object> entry : queryMetadata.serialize().entrySet()) {
			query.put(entry.getKey(), JSONObject.wrap(entry.getValue()));
		}

		return query;
	}

}
