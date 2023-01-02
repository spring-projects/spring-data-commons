/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.data.type.classreading;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.type.MethodsMetadata;
import org.springframework.lang.Nullable;

/**
 * Extension of {@link SimpleMetadataReaderFactory} that reads {@link MethodsMetadata}, creating a new ASM
 * {@link MethodsMetadataReader} for every request.
 *
 * @author Mark Paluch
 * @since 2.1
 * @deprecated since 3.0. Use {@link SimpleMetadataReaderFactory} directly.
 */
@Deprecated
public class MethodsMetadataReaderFactory extends SimpleMetadataReaderFactory {

	/**
	 * Create a new {@link MethodsMetadataReaderFactory} for the default class loader.
	 */
	public MethodsMetadataReaderFactory() {}

	/**
	 * Create a new {@link MethodsMetadataReaderFactory} for the given {@link ResourceLoader}.
	 *
	 * @param resourceLoader the Spring {@link ResourceLoader} to use (also determines the {@link ClassLoader} to use).
	 */
	public MethodsMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	/**
	 * Create a new {@link MethodsMetadataReaderFactory} for the given {@link ClassLoader}.
	 *
	 * @param classLoader the class loader to use.
	 */
	public MethodsMetadataReaderFactory(@Nullable ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	public MethodsMetadataReader getMetadataReader(String className) throws IOException {
		return new MetadataReaderWrapper(super.getMetadataReader(className));
	}

	@Override
	public MethodsMetadataReader getMetadataReader(Resource resource) throws IOException {
		return new MetadataReaderWrapper(super.getMetadataReader(resource));
	}

	private static class MetadataReaderWrapper implements MethodsMetadataReader {

		private final MetadataReader delegate;

		MetadataReaderWrapper(MetadataReader delegate) {
			this.delegate = delegate;
		}

		@Override
		public MethodsMetadata getMethodsMetadata() {
			return new MethodsMetadataWrapper(getAnnotationMetadata(), getClassMetadata());
		}

		@Override
		public Resource getResource() {
			return delegate.getResource();
		}

		@Override
		public ClassMetadata getClassMetadata() {
			return delegate.getClassMetadata();
		}

		@Override
		public AnnotationMetadata getAnnotationMetadata() {
			return delegate.getAnnotationMetadata();
		}

	}

	private static class MethodsMetadataWrapper implements MethodsMetadata, ClassMetadata {

		private final AnnotationMetadata annotationMetadata;
		private final ClassMetadata classMetadata;

		MethodsMetadataWrapper(AnnotationMetadata annotationMetadata, ClassMetadata classMetadata) {
			this.annotationMetadata = annotationMetadata;
			this.classMetadata = classMetadata;
		}

		@Override
		public Set<MethodMetadata> getMethods() {
			return annotationMetadata.getDeclaredMethods();
		}

		@Override
		public Set<MethodMetadata> getMethods(String name) {
			return annotationMetadata.getDeclaredMethods().stream().filter(it -> it.getMethodName().equals(name))
					.collect(Collectors.toSet());
		}

		@Override
		public String getClassName() {
			return classMetadata.getClassName();
		}

		@Override
		public boolean isInterface() {
			return classMetadata.isInterface();
		}

		@Override
		public boolean isAnnotation() {
			return classMetadata.isAnnotation();
		}

		@Override
		public boolean isAbstract() {
			return classMetadata.isAbstract();
		}

		@Override
		public boolean isConcrete() {
			return classMetadata.isConcrete();
		}

		@Override
		public boolean isFinal() {
			return classMetadata.isFinal();
		}

		@Override
		public boolean isIndependent() {
			return classMetadata.isIndependent();
		}

		@Override
		public boolean hasEnclosingClass() {
			return classMetadata.hasEnclosingClass();
		}

		@Override
		@Nullable
		public String getEnclosingClassName() {
			return classMetadata.getEnclosingClassName();
		}

		@Override
		public boolean hasSuperClass() {
			return classMetadata.hasSuperClass();
		}

		@Override
		@Nullable
		public String getSuperClassName() {
			return classMetadata.getSuperClassName();
		}

		@Override
		public String[] getInterfaceNames() {
			return classMetadata.getInterfaceNames();
		}

		@Override
		public String[] getMemberClassNames() {
			return classMetadata.getMemberClassNames();
		}
	}
}
