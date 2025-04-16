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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Christoph Strobl
 */
class LocalVariableNameFactoryUnitTests {

	LocalVariableNameFactory variableNameFactory;

	@BeforeEach
	void beforeEach() {
		variableNameFactory = LocalVariableNameFactory.of(Set.of("firstname", "lastname", "sort"));
	}

	@Test // GH-3270
	void resolvesNameClashesInNames() {

		assertThat(variableNameFactory.generateName("name").name()).isEqualTo("name");
		assertThat(variableNameFactory.generateName("name").name()).isEqualTo("name_1");
		assertThat(variableNameFactory.generateName("name").name()).isEqualTo("name_2");
		assertThat(variableNameFactory.generateName("name1").name()).isEqualTo("name1");
		assertThat(variableNameFactory.generateName("name3").name()).isEqualTo("name3");
		assertThat(variableNameFactory.generateName("name3").name()).isEqualTo("name3_1");
		assertThat(variableNameFactory.generateName("name4_1").name()).isEqualTo("name4_1");
		assertThat(variableNameFactory.generateName("name4").name()).isEqualTo("name4");
		assertThat(variableNameFactory.generateName("name4_1_1").name()).isEqualTo("name4_1_1");
		assertThat(variableNameFactory.generateName("name4_1").name()).isEqualTo("name4_1_2");
		assertThat(variableNameFactory.generateName("name4_1").name()).isEqualTo("name4_1_3");
	}

	@Test // GH-3270
	void considersPredefinedNames() {
		assertThat(variableNameFactory.generateName("firstname").name()).isEqualTo("firstname_1");
	}

	@Test // GH-3270
	void considersCase() {
		assertThat(variableNameFactory.generateName("firstName").name()).isEqualTo("firstName");
	}
}
