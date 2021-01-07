/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.annotation;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.util.AnnotatedTypeScanner;

/**
 * Unit tests for {@link TypeAlias}.
 *
 * @author Oliver Gierke
 */
class TypeAliasUnitTests {

	@Test // DATACMNS-547
	@SuppressWarnings("unchecked")
	void scanningforAtPersistentFindsTypeAliasAnnotatedTypes() {

		AnnotatedTypeScanner scanner = new AnnotatedTypeScanner(Persistent.class);
		Set<Class<?>> types = scanner.findTypes(getClass().getPackage().getName());

		assertThat(types).containsExactlyInAnyOrder(SampleType.class, TypeAlias.class);
	}

	@TypeAlias(value = "foo")
	static class SampleType {

	}
}
