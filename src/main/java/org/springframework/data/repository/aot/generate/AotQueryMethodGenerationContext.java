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
import java.util.Map.Entry;

import javax.lang.model.element.Modifier;

import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationSelectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.repository.aot.generate.VariableNameFactory.VariableName;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.ParameterSpec;
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
	private final CodeBlocks codeBlocks;
	private final VariableNameFactory variableNameFactory;

	AotQueryMethodGenerationContext(RepositoryInformation repositoryInformation, Method method, QueryMethod queryMethod,
			AotRepositoryFragmentMetadata targetTypeMetadata) {

		this.method = method;
		this.annotations = MergedAnnotations.from(method);
		this.queryMethod = queryMethod;
		this.repositoryInformation = repositoryInformation;
		this.targetTypeMetadata = targetTypeMetadata;
		this.targetMethodMetadata = new MethodMetadata(repositoryInformation, method);
		this.codeBlocks = new CodeBlocks(targetTypeMetadata);
		this.variableNameFactory = LocalVariableNameFactory.forMethod(targetMethodMetadata);
	}

	AotRepositoryFragmentMetadata getTargetTypeMetadata() {
		return targetTypeMetadata;
	}

	MethodMetadata getTargetMethodMetadata() {
		return targetMethodMetadata;
	}

	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	public Method getMethod() {
		return method;
	}

	public CodeBlocks codeBlocks() {
		return codeBlocks;
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

	public ResolvableType getActualReturnType() {
		return targetMethodMetadata.getActualReturnType();
	}

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
	 * Suggests naming clash free variant for the given intended variable name within the local method context.
	 *
	 * @param variableName the intended variable name.
	 * @return the suggested VariableName
	 */
	public VariableName suggestLocalVariableName(String variableName) {
		return variableNameFactory.generateName(variableName);
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
					return getParameterName(totalIndex);
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
			parameter.getName().map(result::add);
		}

		return result;
	}

	/**
	 * @return list of all parameter names (including non-bindable special parameters).
	 */
	public List<String> getAllParameterNames() {

		List<String> result = new ArrayList<>();

		for (Parameter parameter : queryMethod.getParameters()) {
			parameter.getName().map(result::add);
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

	public @Nullable String fieldNameOf(Class<?> type) {
		return targetTypeMetadata.fieldNameOf(type);
	}

	@Nullable
	public String getParameterNameOf(Class<?> type) {
		return targetMethodMetadata.getParameterNameOf(type);
	}

	public @Nullable String getParameterName(int position) {

		if (0 > position) {
			return null;
		}

		List<Entry<String, ParameterSpec>> entries = new ArrayList<>(targetMethodMetadata.getMethodArguments().entrySet());
		if (position < entries.size()) {
			return entries.get(position).getKey();
		}
		return null;
	}

	public void addParameter(ParameterSpec parameter) {
		this.targetMethodMetadata.addParameter(parameter);
	}

	@Nullable
	public String getSortParameterName() {
		return getParameterName(queryMethod.getParameters().getSortIndex());
	}

	@Nullable
	public String getPageableParameterName() {
		return getParameterName(queryMethod.getParameters().getPageableIndex());
	}

	@Nullable
	public String getLimitParameterName() {
		return getParameterName(queryMethod.getParameters().getLimitIndex());
	}

}
