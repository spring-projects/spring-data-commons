/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.projection;

import static org.assertj.core.api.Assertions.*;

import java.beans.FeatureDescriptor;
import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * Unit tests for {@link DefaultProjectionInformation}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class DefaultProjectionInformationUnitTests {

	@Test // DATACMNS-89
	void discoversInputProperties() {

		ProjectionInformation information = new DefaultProjectionInformation(CustomerProjection.class);

		assertThat(toNames(information.getInputProperties())).contains("firstname", "lastname");
	}

	@Test // DATACMNS-1206
	void omitsInputPropertiesAcceptingArguments() {

		ProjectionInformation information = new DefaultProjectionInformation(ProjectionAcceptingArguments.class);

		assertThat(toNames(information.getInputProperties())).containsOnly("lastname");
	}

	@Test // DATACMNS-89
	void discoversAllInputProperties() {

		ProjectionInformation information = new DefaultProjectionInformation(ExtendedProjection.class);

		assertThat(toNames(information.getInputProperties())).containsExactly("age", "firstname", "lastname");
	}

	@Test // DATACMNS-1206
	void discoversInputPropertiesInOrder() {

		ProjectionInformation information = new DefaultProjectionInformation(SameMethodNamesInAlternateOrder.class);

		assertThat(toNames(information.getInputProperties())).containsExactly("firstname", "lastname");
	}

	@Test // DATACMNS-1206
	void discoversAllInputPropertiesInOrder() {

		assertThat(toNames(new DefaultProjectionInformation(CompositeProjection.class).getInputProperties()))
				.containsExactly("firstname", "lastname", "age");
		assertThat(toNames(new DefaultProjectionInformation(ReorderedCompositeProjection.class).getInputProperties()))
				.containsExactly("age", "firstname", "lastname");
	}

	@Test // DATACMNS-967
	void doesNotConsiderDefaultMethodInputProperties() {

		ProjectionInformation information = new DefaultProjectionInformation(WithDefaultMethod.class);

		assertThat(information.isClosed()).isTrue();
		assertThat(toNames(information.getInputProperties())).containsExactly("firstname");
	}

	private static List<String> toNames(List<PropertyDescriptor> descriptors) {

		return descriptors.stream()//
				.map(FeatureDescriptor::getName)//
				.distinct()
				.collect(Collectors.toList());
	}

	interface CustomerProjection {

		String getFirstname();

		String getLastname();
	}

	interface ProjectionAcceptingArguments {

		@Value("foo")
		String getFirstname(int i);

		String getLastname();
	}

	interface ExtendedProjection extends CustomerProjection {

		int getAge();
	}

	interface SameMethodNamesInAlternateOrder {

		String getFirstname();

		String getLastname();

		String getFirstname(String foo);
	}

	interface CompositeProjection extends CustomerProjection, AgeProjection {}

	interface ReorderedCompositeProjection extends AgeProjection, CustomerProjection {}

	interface AgeProjection {

		int getAge();
	}

	interface WithDefaultMethod {

		String getFirstname();

		default String getLastname() {
			return null;
		}
	}
}
