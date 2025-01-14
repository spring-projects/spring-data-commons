/*
 * Copyright 2025. the original author or authors.
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

/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.repository.aot.generate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

import javax.lang.model.element.Modifier;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.aot.generate.AotRepositoryBuilder.TargetAotRepositoryImplementationMetadata;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodBuilder.TargetAotRepositoryMethodImplementationMetadata;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.ParameterSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2025/01
 */
public class AotRepositoryMethodGenerationContext {

	private final Method method;
	private final RepositoryInformation repositoryInformation;
	private final TargetAotRepositoryImplementationMetadata targetTypeMetadata;
	private final TargetAotRepositoryMethodImplementationMetadata targetMethodMetadata;
	private final CodeBlocks codeBlocks;

	public AotRepositoryMethodGenerationContext(Method method, RepositoryInformation repositoryInformation,
			TargetAotRepositoryImplementationMetadata targetTypeMetadata) {

		this.method = method;
		this.repositoryInformation = repositoryInformation;
		this.targetTypeMetadata = targetTypeMetadata;
		this.targetMethodMetadata = new TargetAotRepositoryMethodImplementationMetadata();
		this.codeBlocks = new CodeBlocks(targetTypeMetadata);
	}

	public boolean hasField(String fieldName) {
		return targetTypeMetadata.hasField(fieldName);
	}

	public void addField(String fieldName, TypeName type, Modifier... modifiers) {
		targetTypeMetadata.addField(fieldName, type, modifiers);
	}

	public void addField(FieldSpec fieldSpec) {
		targetTypeMetadata.addField(fieldSpec);
	}

	public String fieldNameOf(Class<?> type) {
		return targetTypeMetadata.fieldNameOf(type);
	}

	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	public Method getMethod() {
		return method;
	}

	TargetAotRepositoryImplementationMetadata getTargetTypeMetadata() {
		return targetTypeMetadata;
	}

	@Nullable
	public String getParameterNameOf(Class<?> type) {
		return targetMethodMetadata.getParameterNameOf(type);
	}

	public void addParameter(ParameterSpec parameter) {
		this.targetMethodMetadata.addParameter(parameter);
	}

	public boolean returnsVoid() {
		return getMethod().getReturnType().equals(Void.TYPE);
	}

	public boolean returnsPage() {
		return ClassUtils.isAssignable(Page.class, getMethod().getReturnType());
	}

	public boolean returnsSlice() {
		return ClassUtils.isAssignable(Slice.class, getMethod().getReturnType());
	}

	public boolean returnsCollection() {
		return ClassUtils.isAssignable(Collection.class, getMethod().getReturnType());
	}

	public boolean returnsSingleValue() {
		return !returnsPage() && !returnsSlice() && !returnsCollection();
	}

	public boolean returnsOptionalValue() {
		return ClassUtils.isAssignable(Optional.class, getMethod().getReturnType());
	}

	@Nullable
	public TypeName getActualReturnType() {
		return targetMethodMetadata.actualReturnType;
	}

	@Nullable
	public String getSortParameterName() {
		return getParameterNameOf(Sort.class);
	}

	@Nullable
	public String getPageableParameterName() {
		return getParameterNameOf(Pageable.class);
	}

	@Nullable
	public String getLimitParameterName() {
		return getParameterNameOf(Limit.class);
	}

	@Nullable
	public <T> T annotationValue(Class<? extends Annotation> annotation, String attribute) {
		AnnotationAttributes values = AnnotatedElementUtils.getMergedAnnotationAttributes(getMethod(), annotation);
		return values != null ? (T) values.get(attribute) : null;
	}

	@Nullable
	public TypeName getReturnType() {
		return targetMethodMetadata.getReturnType();
	}

	TargetAotRepositoryMethodImplementationMetadata getTargetMethodMetadata() {
		return targetMethodMetadata;
	}

	public CodeBlocks codeBlocks() {
		return codeBlocks;
	}
}
