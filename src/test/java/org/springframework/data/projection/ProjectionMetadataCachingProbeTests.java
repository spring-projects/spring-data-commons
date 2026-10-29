/*
 * Copyright 2026-present the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Probe reproducing GH-3513: {@link DefaultProjectionInformation} reads the projection type's class-file metadata
 * (via ASM {@code MetadataReader}) once per {@link ProxyProjectionFactory} instance instead of sharing that read
 * across factory instances. In reactive applications this shows up as a Blockhound-flagged blocking file read
 * happening again on the event loop, because the request-time factory (e.g. inside a converter) never benefits from
 * the read already performed on the boot thread by a different factory instance.
 * <p>
 * This test does not require Blockhound/R2DBC to demonstrate the underlying defect: it counts how many times the
 * projection type's {@code .class} resource is actually read from the classpath while resolving
 * {@link ProjectionInformation} through two independent, otherwise identically configured
 * {@link SpelAwareProxyProjectionFactory} instances - mirroring the "bootstrap factory" vs. "converter's own factory"
 * situation described in the issue.
 *
 * @author Raphael Zanarelli
 */
class ProjectionMetadataCachingProbeTests {

	private static final String PROJECTION_CLASS_NAME = GH3513SampleProjection.class.getName();

	@Test // GH-3513
	void sharesClassMetadataReadWithinSingleFactoryInstance() throws Exception {

		ReadCountingClassLoader loader = new ReadCountingClassLoader(getClass().getClassLoader());
		Class<?> isolatedType = loader.loadIsolated(PROJECTION_CLASS_NAME);

		SpelAwareProxyProjectionFactory factory = new SpelAwareProxyProjectionFactory();

		factory.getProjectionInformation(isolatedType);
		factory.getProjectionInformation(isolatedType);

		// Same factory: ProxyProjectionFactory#projectionInformationCache (computeIfAbsent) already avoids a
		// second read for a repeated lookup on the same instance - this must hold both before and after any fix.
		assertThat(loader.getReadCount()).isEqualTo(1);
	}

	@Test // GH-3513
	void reproducesRedundantClassMetadataReadAcrossIndependentFactoryInstances() throws Exception {

		ReadCountingClassLoader loader = new ReadCountingClassLoader(getClass().getClassLoader());
		Class<?> isolatedType = loader.loadIsolated(PROJECTION_CLASS_NAME);

		// Mirrors the reported scenario: one factory resolves the projection at bootstrap (e.g. the repository's
		// own SpelAwareProxyProjectionFactory), a second, independent factory instance resolves the very same
		// projection type again later (e.g. a converter's private factory).
		SpelAwareProxyProjectionFactory bootstrapFactory = new SpelAwareProxyProjectionFactory();
		SpelAwareProxyProjectionFactory requestTimeFactory = new SpelAwareProxyProjectionFactory();

		bootstrapFactory.getProjectionInformation(isolatedType);

		assertThat(loader.getReadCount()) //
				.describedAs("expected exactly one class-file read after the first factory resolved the projection") //
				.isEqualTo(1);

		requestTimeFactory.getProjectionInformation(isolatedType);

		// With the fix (a shared, factory-independent cache for the metadata read) this stays at 1: the second
		// factory should be able to reuse the read already performed by the first one for the very same type.
		assertThat(loader.getReadCount()) //
				.describedAs("second, independent factory instance should reuse the already-read class metadata") //
				.isEqualTo(1);
	}

	/**
	 * A {@link ClassLoader} that defines exactly one class itself (bypassing parent delegation for that single name)
	 * so that {@code type.getClassLoader()} returns this instance, and counts every time that class's {@code .class}
	 * resource is actually read from the classpath.
	 */
	private static class ReadCountingClassLoader extends ClassLoader {

		private final AtomicInteger reads = new AtomicInteger();
		private String isolatedClassName;
		private String isolatedResourcePath;

		ReadCountingClassLoader(ClassLoader parent) {
			super(parent);
		}

		Class<?> loadIsolated(String className) throws ClassNotFoundException {

			this.isolatedClassName = className;
			this.isolatedResourcePath = className.replace('.', '/') + ".class";

			return loadClass(className);
		}

		int getReadCount() {
			return reads.get();
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

			if (!name.equals(isolatedClassName)) {
				return super.loadClass(name, resolve);
			}

			synchronized (getClassLoadingLock(name)) {

				Class<?> loaded = findLoadedClass(name);

				if (loaded == null) {
					loaded = findClass(name);
				}

				if (resolve) {
					resolveClass(loaded);
				}

				return loaded;
			}
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {

			if (!name.equals(isolatedClassName)) {
				return super.findClass(name);
			}

			byte[] bytes = readClassBytes();
			return defineClass(name, bytes, 0, bytes.length);
		}

		private byte[] readClassBytes() throws ClassNotFoundException {

			try (InputStream in = getParent().getResourceAsStream(isolatedResourcePath)) {

				if (in == null) {
					throw new ClassNotFoundException(isolatedClassName);
				}

				return in.readAllBytes();

			} catch (IOException e) {
				throw new ClassNotFoundException(isolatedClassName, e);
			}
		}

		@Override
		public InputStream getResourceAsStream(String name) {

			if (name.equals(isolatedResourcePath)) {
				reads.incrementAndGet();
			}

			return getParent().getResourceAsStream(name);
		}
	}
}
