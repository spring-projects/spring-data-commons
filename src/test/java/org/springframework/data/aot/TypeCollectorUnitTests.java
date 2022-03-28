/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.aot;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.aot.types.*;

/**
 * @author Christoph Strobl
 */
public class TypeCollectorUnitTests {

	@Test // GH-2593
	void detectsSignatureTypes() {
		assertThat(TypeCollector.inspect(FieldsAndMethods.class).list()).containsExactlyInAnyOrder(FieldsAndMethods.class,
				AbstractType.class, InterfaceType.class);
	}

	@Test // GH-2593
	void detectsMethodArgs() {
		assertThat(TypeCollector.inspect(TypesInMethodSignatures.class).list())
				.containsExactlyInAnyOrder(TypesInMethodSignatures.class, EmptyType1.class, EmptyType2.class);
	}

	@Test // GH-2593
	void doesNotOverflowOnCyclicPropertyReferences() {
		assertThat(TypeCollector.inspect(CyclicPropertiesA.class).list()).containsExactlyInAnyOrder(CyclicPropertiesA.class,
				CyclicPropertiesB.class);
	}

	@Test
	void doesNotOverflowOnCyclicSelfReferences() {
		assertThat(TypeCollector.inspect(CyclicPropertiesSelf.class).list())
				.containsExactlyInAnyOrder(CyclicPropertiesSelf.class);
	}

	@Test
	void doesNotOverflowOnCyclicGenericsReferences() {
		assertThat(TypeCollector.inspect(CyclicGenerics.class).list()).containsExactlyInAnyOrder(CyclicGenerics.class);
	}

	@Test
	void includesDeclaredClassesInInspection() {
		assertThat(TypeCollector.inspect(WithDeclaredClass.class).list()).containsExactlyInAnyOrder(WithDeclaredClass.class,
				WithDeclaredClass.SomeEnum.class);
	}

}
