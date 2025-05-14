/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.repository.aot.generate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationSelectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.javapoet.TypeName;
import org.springframework.util.ObjectUtils;

/**
 * Generational AOT context for repository query method generation.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
public class AotQueryMethodGenerationContext {

	private final Method method;
	private final MergedAnnotations annotations;
	private final QueryMethod queryMethod;
	private final RepositoryInformation repositoryInformation;
	private final AotRepositoryFragmentMetadata targetTypeMetadata;
	private final MethodMetadata targetMethodMetadata;
	private final VariableNameFactory variableNameFactory;

	AotQueryMethodGenerationContext(RepositoryInformation repositoryInformation, Method method, QueryMethod queryMethod,
			AotRepositoryFragmentMetadata targetTypeMetadata) {

		this.method = method;
		this.annotations = MergedAnnotations.from(method);
		this.queryMethod = queryMethod;
		this.repositoryInformation = repositoryInformation;
		this.targetTypeMetadata = targetTypeMetadata;
		this.targetMethodMetadata = new MethodMetadata(repositoryInformation, method);
		this.variableNameFactory = LocalVariableNameFactory.forMethod(targetMethodMetadata);
	}

	MethodMetadata getTargetMethodMetadata() {
		return targetMethodMetadata;
	}

	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	/**
	 * Obtain the field name by type.
	 *
	 * @param type
	 * @return
	 */
	public @Nullable String fieldNameOf(Class<?> type) {
		return targetTypeMetadata.fieldNameOf(type);
	}

	public Method getMethod() {
		return method;
	}

	/**
	 * @return the {@link MergedAnnotations} that are present on the method.
	 */
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
	 * @return the actual returned domain type.
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getReturnedDomainClass(Method)
	 */
	public ResolvableType getActualReturnType() {
		return targetMethodMetadata.getActualReturnType();
	}

	/**
	 * @return the query method return type.
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getReturnType(Method)
	 */
	public ResolvableType getReturnType() {
		return targetMethodMetadata.getReturnType();
	}

	/**
	 * @return the {@link TypeName} representing the method return type.
	 */
	public TypeName getReturnTypeName() {
		return TypeName.get(getReturnType().getType());
	}

	/**
	 * Returns the required parameter name for the {@link Parameter#isBindable() bindable parameter} at the given
	 * {@code parameterIndex} or throws {@link IllegalArgumentException} if the parameter cannot be determined by its
	 * index.
	 *
	 * @param parameterIndex the zero-based parameter index as used in the query to reference bindable parameters.
	 * @return the method parameter name.
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
	 * @return the method parameter name.
	 */
	public @Nullable String getBindableParameterName(int parameterIndex) {

		int bindable = 0;
		int totalIndex = 0;
		for (Parameter parameter : queryMethod.getParameters()) {

			if (parameter.isBindable()) {

				if (bindable == parameterIndex) {
					return getParameterName(totalIndex);
				}
				bindable++;
			}

			totalIndex++;
		}

		return null;
	}

	/**
	 * Returns the required parameter name for the {@link Parameter#isBindable() bindable parameter} at the given logical
	 * {@code parameterName} or throws {@link IllegalArgumentException} if the parameter cannot be determined by its name.
	 *
	 * @param parameterName the parameter name as used in the query to reference bindable parameters.
	 * @return the method parameter name.
	 * @see org.springframework.data.repository.query.Param
	 */
	public String getRequiredBindableParameterName(String parameterName) {

		String name = getBindableParameterName(parameterName);

		if (ObjectUtils.isEmpty(name)) {
			throw new IllegalArgumentException("No bindable parameter with name '%s'".formatted(parameterName));
		}

		return name;
	}

	/**
	 * Returns the required parameter name for the {@link Parameter#isBindable() bindable parameter} at the given logical
	 * {@code parameterName} or {@code null} if the parameter cannot be determined by its name.
	 *
	 * @param parameterName the parameter name as used in the query to reference bindable parameters.
	 * @return the method parameter name.
	 * @see org.springframework.data.repository.query.Param
	 */
	public @Nullable String getBindableParameterName(String parameterName) {

		int totalIndex = 0;
		for (Parameter parameter : queryMethod.getParameters()) {

			totalIndex++;
			if (!parameter.isBindable()) {
				continue;
			}

			if (parameter.getName().filter(it -> it.equals(parameterName)).isPresent()) {
				return getParameterName(totalIndex - 1);
			}
		}

		return null;
	}

	/**
	 * @return list of bindable parameter names.
	 */
	public List<String> getBindableParameterNames() {

		List<String> result = new ArrayList<>();

		for (Parameter parameter : queryMethod.getParameters().getBindableParameters()) {
			result.add(getParameterName(parameter.getIndex()));
		}

		return result;
	}

	/**
	 * @return list of all parameter names (including non-bindable special parameters).
	 */
	public List<String> getAllParameterNames() {
		return targetMethodMetadata.getMethodArguments().keySet().stream().toList();
	}

	/**
	 * Obtain a naming-clash free variant for the given logical variable name within the local method context. Returns the
	 * target variable name when called multiple times with the same {@code variableName}.
	 *
	 * @param variableName the logical variable name.
	 * @return the variable name used in the generated code.
	 */
	public String localVariable(String variableName) {
		return targetMethodMetadata.getLocalVariables().computeIfAbsent(variableName, variableNameFactory::generateName);
	}

	/**
	 * Returns the parameter name for the method parameter at {@code position}.
	 *
	 * @param position zero-indexed parameter position.
	 * @return
	 * @see Method#getParameters()
	 */
	public @Nullable String getParameterName(int position) {
		return targetMethodMetadata.getParameterName(position);
	}

	/**
	 * @return the parameter name for the {@link org.springframework.data.domain.Sort sort parameter} or {@code null} if
	 *         the method does not declare a sort parameter.
	 */
	@Nullable
	public String getSortParameterName() {
		return getParameterName(queryMethod.getParameters().getSortIndex());
	}

	/**
	 * @return the parameter name for the {@link org.springframework.data.domain.Pageable pageable parameter} or
	 *         {@code null} if the method does not declare a pageable parameter.
	 */
	@Nullable
	public String getPageableParameterName() {
		return getParameterName(queryMethod.getParameters().getPageableIndex());
	}

	/**
	 * @return the parameter name for the {@link org.springframework.data.domain.Limit limit parameter} or {@code null} if
	 *         the method does not declare a limit parameter.
	 */
	@Nullable
	public String getLimitParameterName() {
		return getParameterName(queryMethod.getParameters().getLimitIndex());
	}

	/**
	 * @return the parameter name for the {@link org.springframework.data.domain.ScrollPosition scroll position parameter}
	 *         or {@code null} if the method does not declare a scroll position parameter.
	 */
	@Nullable
	public String getScrollPositionParameterName() {
		return getParameterName(queryMethod.getParameters().getScrollPositionIndex());
	}

	/**
	 * @return the parameter name for the {@link Class dynamic projection parameter} or {@code null} if the method does
	 *         not declare a dynamic projection parameter.
	 */
	@Nullable
	public String getDynamicProjectionParameterName() {
		return getParameterName(queryMethod.getParameters().getDynamicProjectionIndex());
	}

}
