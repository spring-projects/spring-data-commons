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

import lombok.Getter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.asm.ClassReader;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.AnnotationMetadataReadingVisitor;
import org.springframework.core.type.classreading.MethodMetadataReadingVisitor;
import org.springframework.data.type.MethodsMetadata;
import org.springframework.util.Assert;

/**
 * {@link MethodsMetadataReader} implementation based on an ASM {@link org.springframework.asm.ClassReader}.
 *
 * @author Mark Paluch
 * @auhtor Oliver Gierke
 * @since 2.1
 * @since 1.11.11
 */
@Getter
class DefaultMethodsMetadataReader implements MethodsMetadataReader {

	private final Resource resource;
	private final ClassMetadata classMetadata;
	private final AnnotationMetadata annotationMetadata;
	private final MethodsMetadata methodsMetadata;

	DefaultMethodsMetadataReader(Resource resource, ClassLoader classLoader) throws IOException {

		this.resource = resource;

		ClassReader classReader = null;
		InputStream is = null;

		try {

			is = new BufferedInputStream(getResource().getInputStream());
			classReader = new ClassReader(is);

		} catch (IllegalArgumentException ex) {

			throw new NestedIOException("ASM ClassReader failed to parse class file - "
					+ "probably due to a new Java class file version that isn't supported yet: " + getResource(), ex);

		} finally {

			if (is != null) {
				is.close();
			}
		}

		MethodsMetadataReadingVisitor visitor = new MethodsMetadataReadingVisitor(classLoader);
		classReader.accept(visitor, ClassReader.SKIP_DEBUG);

		classMetadata = visitor;
		annotationMetadata = visitor;
		methodsMetadata = visitor;
	}

	/**
	 * ASM class visitor which looks for the class name and implemented types as well as for the methods defined in the
	 * class, exposing them through the {@link MethodsMetadata} interface.
	 *
	 * @author Mark Paluch
	 * @since 2.1
	 * @since 1.11.11
	 * @see ClassMetadata
	 * @see MethodMetadata
	 * @see MethodMetadataReadingVisitor
	 */
	static class MethodsMetadataReadingVisitor extends AnnotationMetadataReadingVisitor implements MethodsMetadata {

		/**
		 * Construct a new {@link MethodsMetadataReadingVisitor} given {@link ClassLoader}.
		 *
		 * @param classLoader may be {@literal null}.
		 */
		MethodsMetadataReadingVisitor(ClassLoader classLoader) {
			super(classLoader);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.classreading.AnnotationMetadataReadingVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
		 */
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

			// Skip bridge methods - we're only interested in original user methods.
			// On JDK 8, we'd otherwise run into double detection of the same method...
			if ((access & Opcodes.ACC_BRIDGE) != 0) {
				return super.visitMethod(access, name, desc, signature, exceptions);
			}

			// Skip constructors
			if (name.equals("<init>")) {
				return super.visitMethod(access, name, desc, signature, exceptions);
			}

			MethodMetadataReadingVisitor visitor = new MethodMetadataReadingVisitor(name, access, getClassName(),
					Type.getReturnType(desc).getClassName(), this.classLoader, this.methodMetadataSet);

			this.methodMetadataSet.add(visitor);
			return visitor;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.MethodsMetadata#getMethods()
		 */
		@Override
		public Set<MethodMetadata> getMethods() {
			return Collections.unmodifiableSet(methodMetadataSet);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.MethodsMetadata#getMethods(String)
		 */
		@Override
		public Set<MethodMetadata> getMethods(String name) {

			Assert.hasText(name, "Method name must not be null or empty");

			Set<MethodMetadata> result = new LinkedHashSet<MethodMetadata>(4);

			for (MethodMetadata metadata : methodMetadataSet) {
				if (metadata.getMethodName().equals(name)) {
					result.add(metadata);
				}
			}

			return Collections.unmodifiableSet(result);
		}
	}
}
