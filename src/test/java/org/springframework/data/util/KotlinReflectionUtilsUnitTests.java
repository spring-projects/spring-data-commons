/*
 * Copyright 2020-2021 the original author or authors.
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
import static org.assertj.core.api.Assumptions.*;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.KotlinDetector;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link KotlinReflectionUtilsUnitTests}.
 *
 * @author Mark Paluch
 */
public class KotlinReflectionUtilsUnitTests {

	@BeforeEach
	void before() {
		assumeThat(Version.javaVersion()).isLessThan(Version.parse("9.0"));
	}

	@Test // DATACMNS-1508
	void classShouldLoadWithKotlin() {
		assertThat(KotlinDetector.isKotlinPresent()).isTrue();
		assertThat(KotlinReflectionUtils.isSupportedKotlinClass(TypeCreatingSyntheticClass.class)).isTrue();
	}

	@Test // DATACMNS-1508
	void classShouldLoadWithoutKotlin() throws Exception {
		runTest("loadClassWithoutKotlin");
	}

	// executed via reflection in the context of a ClassLoader without Kotlin dependencies.
	public void loadClassWithoutKotlin() {

		assertThat(KotlinDetector.isKotlinPresent()).isFalse();
		assertThat(KotlinReflectionUtils.isSupportedKotlinClass(TypeCreatingSyntheticClass.class)).isFalse();
	}

	void runTest(String testName)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException {

		KotlinExcludingURLClassLoader classLoader = new KotlinExcludingURLClassLoader(
				((URLClassLoader) getClass().getClassLoader()).getURLs());
		Class<?> testClass = ClassUtils.forName(getClass().getName(), classLoader);

		ReflectionUtils.invokeMethod(testClass.getMethod(testName), testClass.newInstance());
	}

	static class KotlinExcludingURLClassLoader extends URLClassLoader {
		KotlinExcludingURLClassLoader(URL[] urls) {
			super(urls, null);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {

			if (name.startsWith("kotlin")) {
				throw new ClassNotFoundException("Denied: " + name);
			}

			return super.findClass(name);
		}
	}
}
