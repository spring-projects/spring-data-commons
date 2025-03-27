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
import java.util.Map.Entry;
import java.util.Optional;

import javax.lang.model.element.Modifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
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
	private final QueryMethod queryMethod;
	private final RepositoryInformation repositoryInformation;
	private final AotRepositoryFragmentMetadata targetTypeMetadata;
	private final AotRepositoryMethodImplementationMetadata targetMethodMetadata;
	private final CodeBlocks codeBlocks;
	private final @Nullable PartTree partTree;

	public AotRepositoryMethodGenerationContext(RepositoryInformation repositoryInformation, Method method,
			QueryMethod queryMethod, AotRepositoryFragmentMetadata targetTypeMetadata) {

		this.method = method;
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

	QueryMethod getQueryMethod() {
		return queryMethod;
	}

	public ReturnedType getReturnedType() {
		return queryMethod.getResultProcessor().getReturnedType();
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

	public String getParameterNameOfPosition(int position) {

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
