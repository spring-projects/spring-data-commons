/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.type.classreading;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.asm.ClassReader;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.data.type.MethodsMetadata;
import org.springframework.data.type.MethodsMetadataReader;
import org.springframework.lang.Nullable;

/**
 * {@link MethodsMetadataReader} implementation based on an ASM {@link org.springframework.asm.ClassReader}.
 *
 * @author Mark Paluch
 * @since 2.1
 */
class DefaultMethodsMetadataReader implements MethodsMetadataReader {

	private final Resource resource;
	private final ClassMetadata classMetadata;
	private final AnnotationMetadata annotationMetadata;
	private final MethodsMetadata methodsMetadata;

	DefaultMethodsMetadataReader(Resource resource, @Nullable ClassLoader classLoader) throws IOException {

		this.resource = resource;

		ClassReader classReader;

		try (InputStream is = new BufferedInputStream(getResource().getInputStream())) {
			classReader = new ClassReader(is);
		} catch (IllegalArgumentException ex) {
			throw new NestedIOException("ASM ClassReader failed to parse class file - "
					+ "probably due to a new Java class file version that isn't supported yet: " + getResource(), ex);
		}

		MethodsMetadataReadingVisitor visitor = new MethodsMetadataReadingVisitor(classLoader);
		classReader.accept(visitor, ClassReader.SKIP_DEBUG);

		classMetadata = visitor;
		annotationMetadata = visitor;
		methodsMetadata = visitor;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.type.classreading.MetadataReader#getResource()
	 */
	@Override
	public Resource getResource() {
		return resource;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.type.classreading.MetadataReader#getClassMetadata()
	 */
	@Override
	public ClassMetadata getClassMetadata() {
		return classMetadata;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.type.classreading.MetadataReader#getAnnotationMetadata()
	 */
	@Override
	public AnnotationMetadata getAnnotationMetadata() {
		return annotationMetadata;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.util.ClassMetadataReader#getMethodsMetadata()
	 */
	@Override
	public MethodsMetadata getMethodsMetadata() {
		return methodsMetadata;
	}
}
