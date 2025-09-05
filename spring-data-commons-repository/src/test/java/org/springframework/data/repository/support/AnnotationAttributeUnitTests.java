/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

/**
 * Unit tests for {@link AnnotationAttribute}.
 *
 * @author Oliver Gierke
 */
class AnnotationAttributeUnitTests {

	@Test // DATACMNS-607
	void rejectsNullAnnotationType() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AnnotationAttribute(null));
	}

	@Test // DATACMNS-607
	void rejectsNullAnnotationTypeForAnnotationAndAttributeName() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AnnotationAttribute(null, Optional.of("name")));
	}

	@Test // DATACMNS-607
	void looksUpAttributeFromAnnotatedElement() {

		var attribute = new AnnotationAttribute(Component.class);
		assertThat(attribute.getValueFrom(Sample.class)).hasValue("foo");
	}

	@Component("foo")
	static class Sample {}
}
