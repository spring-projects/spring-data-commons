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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.lang.model.element.Modifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationSelectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.ParameterSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @since 2025/01
 */
public class AotRepositoryMethodGenerationContext {

	private final Method method;
	private final MergedAnnotations annotations;
	private final QueryMethod queryMethod;
	private final RepositoryInformation repositoryInformation;
	private final AotRepositoryFragmentMetadata targetTypeMetadata;
	private final AotRepositoryMethodImplementationMetadata targetMethodMetadata;
	private final CodeBlocks codeBlocks;
	private final @Nullable PartTree partTree;

	AotRepositoryMethodGenerationContext(RepositoryInformation repositoryInformation, Method method,
			QueryMethod queryMethod, AotRepositoryFragmentMetadata targetTypeMetadata) {

		this.method = method;
		this.annotations = MergedAnnotations.from(method);
		this.queryMethod = queryMethod;
		this.repositoryInformation = repositoryInformation;
		this.targetTypeMetadata = targetTypeMetadata;
		this.targetMethodMetadata = new AotRepositoryMethodImplementationMetadata(repositoryInformation, method);
		this.codeBlocks = new CodeBlocks(targetTypeMetadata);

		PartTree partTree = null;
		try {
			partTree = new PartTree(method.getName(), repositoryInformation.getDomainType());
		} catch (Exception e) {
			// not a part tree quer
		}

		this.partTree = partTree;
	}

	public MergedAnnotations getAnnotations() {
		return annotations;
	}

	/**
	 * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching annotation or meta-annotation of the
	 * specified type, or {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the annotation type to get
	 * @return a {@link MergedAnnotation} instance
	 */
	public <A extends Annotation> MergedAnnotation<A> getAnnotation(Class<A> annotationType) {
		return annotations.get(annotationType);
	}

	/**
	 * @return the returned type without considering dynamic projections.
	 */
	public ReturnedType getReturnedType() {
		return queryMethod.getResultProcessor().getReturnedType();
	}

	/**
	 * Returns the required parameter name for the {@link Parameter#isBindable() bindable parameter} at the given
	 * {@code parameterIndex} or throws {@link IllegalArgumentException} if the parameter cannot be determined by its
	 * index.
	 *
	 * @param parameterIndex the zero-based parameter index as used in the query to reference bindable parameters.
	 * @return the parameter name.
	 */
	public String getRequiredBindableParameterName(int parameterIndex) {

		String name = getBindableParameterName(parameterIndex);

		if (ObjectUtils.isEmpty(name)) {
			throw new IllegalArgumentException("No bindable parameter with index %d".formatted(parameterIndex));
		}

		return name;
	}

	/**
	 * Returns the parameter name for the {@link Parameter#isBindable() bindable parameter} at the given
	 * {@code parameterIndex} or {@code null} if the parameter cannot be determined by its index.
	 *
	 * @param parameterIndex the zero-based parameter index as used in the query to reference bindable parameters.
	 * @return the parameter name.
	 */
	// TODO: Simplify?!
	public @Nullable String getBindableParameterName(int parameterIndex) {

		int bindable = 0;
		int totalIndex = 0;
		for (Parameter parameter : queryMethod.getParameters()) {

			if (parameter.isBindable()) {

				if (bindable == parameterIndex) {
					return getParameterNameOfPosition(totalIndex);
				}
				bindable++;
			}

			totalIndex++;
		}

		return null;
	}

	/**
	 * Returns the required parameter name for the {@link Parameter#isBindable() bindable parameter} at the given
	 * {@code parameterName} or throws {@link IllegalArgumentException} if the parameter cannot be determined by its
	 * index.
	 *
	 * @param parameterName the parameter name as used in the query to reference bindable parameters.
	 * @return the parameter name.
	 */
	public String getRequiredBindableParameterName(String parameterName) {

		String name = getBindableParameterName(parameterName);

		if (ObjectUtils.isEmpty(name)) {
			throw new IllegalArgumentException("No bindable parameter with name '%s'".formatted(parameterName));
		}

		return name;
	}

	/**
	 * Returns the required parameter name for the {@link Parameter#isBindable() bindable parameter} at the given
	 * {@code parameterName} or {@code null} if the parameter cannot be determined by its index.
	 *
	 * @param parameterName the parameter name as used in the query to reference bindable parameters.
	 * @return the parameter name.
	 */
	// TODO: Simplify?!
	public @Nullable String getBindableParameterName(String parameterName) {

		int totalIndex = 0;
		for (Parameter parameter : queryMethod.getParameters()) {

			totalIndex++;
			if (!parameter.isBindable()) {
				continue;
			}

			if (parameter.getName().filter(it -> it.equals(parameterName)).isPresent()) {
				return getParameterNameOfPosition(totalIndex - 1);
			}
		}

		return null;
	}

	public List<String> getBindableParameterNames() {

		List<String> result = new ArrayList<>();

		for (Parameter parameter : queryMethod.getParameters().getBindableParameters()) {

			if (parameter.isBindable()) {
				parameter.getName().map(result::add);
			}
		}

		return result;
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

	AotRepositoryFragmentMetadata getTargetTypeMetadata() {
		return targetTypeMetadata;
	}

	@Nullable
	public String getParameterNameOf(Class<?> type) {
		return targetMethodMetadata.getParameterNameOf(type);
	}

	public @Nullable String getParameterNameOfPosition(int position) {

		ArrayList<Entry<String, ParameterSpec>> entries = new ArrayList<>(
				targetMethodMetadata.getMethodArguments().entrySet());
		if (position < entries.size()) {
			return entries.get(position).getKey();
		}
		return null;
	}

	public void addParameter(ParameterSpec parameter) {
		this.targetMethodMetadata.addParameter(parameter);
	}


	public boolean returnsVoid() {
		return ClassUtils.isVoidType(getReturnType().getRawClass());
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

	public boolean isCountMethod() {
		return partTree != null ? partTree.isCountProjection() : method.getName().startsWith("count");
	}

	public boolean isExistsMethod() {
		return partTree != null ? partTree.isExistsProjection() : method.getName().startsWith("exists");
	}

	public boolean isDeleteMethod() {
		return partTree != null ? partTree.isDelete() : method.getName().startsWith("delete");
	}

	public ResolvableType getActualReturnType() {
		return targetMethodMetadata.getActualReturnType();
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

	public ResolvableType getReturnType() {
		return targetMethodMetadata.getReturnType();
	}

	AotRepositoryMethodImplementationMetadata getTargetMethodMetadata() {
		return targetMethodMetadata;
	}

	public CodeBlocks codeBlocks() {
		return codeBlocks;
	}

}
