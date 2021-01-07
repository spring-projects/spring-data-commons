/*
 * Copyright 2019-2021 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

/**
 * Unit test for {@link ParameterTypes}.
 *
 * @author Oliver Drotbohm
 */
public class ParameterTypesUnitTests {

	@Test // DATACMNS-1518
	public void detectsDirectMatch() {

		Method method = ReflectionUtils.findMethod(Sample.class, "twoStrings", String.class, String.class);

		ParameterTypes types = ParameterTypes.of(String.class, String.class);

		assertThat(types.areValidFor(method)).isTrue();
		assertThat(types.exactlyMatchParametersOf(method)).isTrue();
	}

	@Test // DATACMNS-1518
	public void supportsSimpleVarArg() {

		Method method = ReflectionUtils.findMethod(Sample.class, "stringPlusStringVarArg", String.class, String[].class);

		ParameterTypes types = ParameterTypes.of(String.class, String.class);

		assertThat(types.areValidFor(method)).isTrue();
		assertThat(types.exactlyMatchParametersOf(method)).isFalse();
	}

	@Test // DATACMNS-1518
	public void supportsTrailingObjectVarArg() {

		Method method = ReflectionUtils.findMethod(Sample.class, "stringPlusObjectVarArg", String.class, Object[].class);

		ParameterTypes types = ParameterTypes.of(String.class, String.class);

		assertThat(types.areValidFor(method)).isTrue();
		assertThat(types.exactlyMatchParametersOf(method)).isFalse();
	}

	@Test // DATACMNS-1518
	public void supportsObjectVarArg() {

		Method method = ReflectionUtils.findMethod(Sample.class, "objectVarArg", Object[].class);

		ParameterTypes types = ParameterTypes.of(String.class, String.class);

		assertThat(types.areValidFor(method)).isTrue();
		assertThat(types.exactlyMatchParametersOf(method)).isFalse();

	}

	@Test // DATACMNS-1518
	public void doesNotAddNonObjectVarArgsForParents() {

		ParameterTypes types = ParameterTypes.of(String.class, String.class, Integer.class, Integer.class);

		List<ParameterTypes> alternatives = types.getAllAlternatives();

		assertThat(alternatives).hasSize(6);

		assertThat(alternatives).anyMatch(it -> it.hasTypes(String.class, String.class, Integer.class, Integer[].class));
		assertThat(alternatives).anyMatch(it -> it.hasTypes(String.class, String.class, Integer.class, Object[].class));
		assertThat(alternatives).anyMatch(it -> it.hasTypes(String.class, String.class, Integer[].class));
		assertThat(alternatives).anyMatch(it -> it.hasTypes(String.class, String.class, Object[].class));
		assertThat(alternatives).anyMatch(it -> it.hasTypes(String.class, Object[].class));
		assertThat(alternatives).anyMatch(it -> it.hasTypes(Object[].class));
	}

	interface Sample {

		void twoStrings(String first, String second);

		void stringPlusStringVarArg(String first, String... second);

		void stringPlusObjectVarArg(String first, Object... second);

		void objectVarArg(Object... args);
	}
}
