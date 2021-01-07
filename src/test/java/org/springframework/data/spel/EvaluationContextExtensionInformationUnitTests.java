/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.spel;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.spel.spi.EvaluationContextExtension;

/**
 * Unit tests for {@link EvaluationContextExtensionInformation}.
 *
 * @author Oliver Gierke
 */
class EvaluationContextExtensionInformationUnitTests {

	@Test // DATACMNS-1024
	void supportsMethodOverloadsOnRoot() {

		EvaluationContextExtensionInformation information = new EvaluationContextExtensionInformation(
				SampleExtension.class);

		Optional<Object> target = Optional.of(new Object());

		information.getRootObjectInformation(target).getFunctions(target);
	}

	interface SampleExtension extends EvaluationContextExtension {

		@Override
		SampleRoot getRootObject();
	}

	interface SampleRoot {

		void someMethod();

		void someMethod(String parameter);
	}
}
