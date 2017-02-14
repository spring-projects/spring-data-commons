/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.classloadersupport;

import static java.util.Arrays.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * supports creation of tests that need to load classes with a {@link HidingClassLoader}.
 *
 * @author Jens Schauder
 */
public class ClassLoaderRule implements MethodRule {

	public HidingClassLoader classLoader;

	@Override
	public Statement apply(final Statement base, FrameworkMethod method, Object target) {

		CombinedClassLoaderConfiguration combinedConfiguration = new CombinedClassLoaderConfiguration(
				method.getAnnotation(ClassLoaderConfiguration.class),
				method.getDeclaringClass().getAnnotation(ClassLoaderConfiguration.class)
		);

		classLoader = createClassLoader(combinedConfiguration);

		return new Statement() {

			@Override
			public void evaluate() throws Throwable {

				try {
					base.evaluate();
				} finally {
					classLoader = null;
				}
			}
		};
	}

	private static HidingClassLoader createClassLoader(CombinedClassLoaderConfiguration configuration) {

		HidingClassLoader classLoader = new HidingClassLoader(mergeHidden(configuration));

		for (Class shadow : configuration.shadowPackages) {
			classLoader.excludeClass(shadow.getPackage().getName());
		}

		for (String shadow : configuration.shadowByPrefix) {
			classLoader.excludePackage(shadow);
		}

		return classLoader;
	}

	private static List<String> mergeHidden(CombinedClassLoaderConfiguration configuration) {

		List<String> hidden = new ArrayList<String>();

		for (Class aClass : configuration.hidePackages) {
			hidden.add(aClass.getPackage().getName());
		}

		for (String aPackage : configuration.hideByPrefix) {
			hidden.add(aPackage);
		}

		return hidden;
	}

	private static class CombinedClassLoaderConfiguration {

		final List<Class> shadowPackages = new ArrayList<Class>();
		final List<String> shadowByPrefix = new ArrayList<String>();
		final List<Class> hidePackages = new ArrayList<Class>();
		final List<String> hideByPrefix = new ArrayList<String>();

		CombinedClassLoaderConfiguration(ClassLoaderConfiguration methodAnnotation, ClassLoaderConfiguration classAnnotation) {

			mergeAnnotation(methodAnnotation);
			mergeAnnotation(classAnnotation);
		}

		private void mergeAnnotation(ClassLoaderConfiguration methodAnnotation) {

			if (methodAnnotation != null) {

				shadowPackages.addAll(asList(methodAnnotation.shadowPackage()));
				shadowByPrefix.addAll(asList(methodAnnotation.shadowByPrefix()));
				hidePackages.addAll(asList(methodAnnotation.hidePackage()));
				hideByPrefix.addAll(asList(methodAnnotation.hideByPrefix()));
			}
		}
	}
}
