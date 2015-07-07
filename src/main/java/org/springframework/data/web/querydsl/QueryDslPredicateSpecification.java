/*
 * Copyright 2015 the original author or authors.
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
 */
package org.springframework.data.web.querydsl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Christoph Strobl
 * @since 1.11
 */
public class QueryDslPredicateSpecification {

	private final Map<String, QueryDslPredicateBuilder> pathSpecs;

	public QueryDslPredicateSpecification() {
		this.pathSpecs = new LinkedHashMap<String, QueryDslPredicateBuilder>();
	}

	public Map<String, QueryDslPredicateBuilder> getSpecs() {
		return this.pathSpecs;
	}

	public void define(String path, QueryDslPredicateBuilder builder) {
		this.pathSpecs.put(path, builder);
	}

	// TODO: does it make sense to offer methods like upper, lower, gt, lt,...
}
