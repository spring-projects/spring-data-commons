/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.repository.init;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link ResourceReader} using Jackson to read JSON into objects.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @since 4.0
 */
public class JacksonResourceReader implements ResourceReader {

	private static final String DEFAULT_TYPE_KEY = "_class";
	private static final ObjectMapper DEFAULT_MAPPER = JsonMapper.builder()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();

	private final ObjectMapper mapper;
	private String typeKey = DEFAULT_TYPE_KEY;

	/**
	 * Creates a new {@link JacksonResourceReader}.
	 */
	public JacksonResourceReader() {
		this(DEFAULT_MAPPER);
	}

	/**
	 * Creates a new {@link JacksonResourceReader} using the given {@link ObjectMapper}.
	 *
	 * @param mapper
	 */
	public JacksonResourceReader(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Configures the JSON document's key to look up the type to instantiate the object. Defaults to
	 * {@link JacksonResourceReader#DEFAULT_TYPE_KEY}.
	 *
	 * @param typeKey
	 */
	public void setTypeKey(@Nullable String typeKey) {
		this.typeKey = typeKey == null ? DEFAULT_TYPE_KEY : typeKey;
	}

	@Override
	public Object readFrom(Resource resource, @Nullable ClassLoader classLoader) throws Exception {

		Assert.notNull(resource, "Resource must not be null");

		InputStream stream = resource.getInputStream();
		JsonNode node = mapper.readerFor(JsonNode.class).readTree(stream);

		if (node.isArray()) {

			Iterator<JsonNode> elements = node.iterator();
			List<Object> result = new ArrayList<>();

			while (elements.hasNext()) {
				JsonNode element = elements.next();
				result.add(readSingle(element, classLoader));
			}

			return result;
		}

		return readSingle(node, classLoader);
	}

	/**
	 * Reads the given {@link JsonNode} into an instance of the type encoded in it using the configured type key.
	 *
	 * @param node must not be {@literal null}.
	 * @param classLoader can be {@literal null}.
	 * @return
	 */
	private Object readSingle(JsonNode node, @Nullable ClassLoader classLoader) throws IOException {

		JsonNode typeNode = node.findValue(typeKey);

		if (typeNode == null) {
			throw new IllegalArgumentException(String.format("Could not find type for type key '%s'", typeKey));
		}

		String typeName = typeNode.asString();
		Class<?> type = ClassUtils.resolveClassName(typeName, classLoader);

		return mapper.readerFor(type).readValue(node);
	}
}
