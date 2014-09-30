/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.repository.inmemory.hazelcast;

import org.springframework.data.repository.inmemory.GenericInMemoryOperationsUnitTests;
import org.springframework.data.repository.inmemory.InMemoryOperations;
import org.springframework.data.repository.inmemory.InMemoryQuery;

import com.hazelcast.query.EntryObject;
import com.hazelcast.query.PredicateBuilder;

/**
 * @author Christoph Strobl
 */
public class HazelcastOperationsUnitTests extends GenericInMemoryOperationsUnitTests {

	@Override
	protected InMemoryOperations getInMemoryOperations() {
		return new HazelcastTemplate(HazelcastUtils.preconfiguredHazelcastAdapter());
	}

	@Override
	protected InMemoryQuery getInMemoryQuery() {
		EntryObject e = new PredicateBuilder().getEntryObject();
		return new HazelcastQuery(e.get("foo").equal("two"));
	}

}
