/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.aot;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.repository.core.RepositoryInformation;

/**
 * @author Christoph Strobl
 */
public interface AotRepositoryContext extends AotContext {

	/**
	 * @return the bean name of the repository / factory bean
	 */
	String getBeanName();

	/**
	 * @return base packages to look for repositories.
	 */
	Set<String> getBasePackages();

	/**
	 * @return metadata about the repository itself
	 */
	RepositoryInformation getRepositoryInformation();

	/**
	 * @return the {@link Annotation} types used to identify domain types.
	 */
	Set<Class<? extends Annotation>> getIdentifyingAnnotations();

	/**
	 * @return all types reachable from the repository.
	 */
	Set<Class<?>> getResolvedTypes();

	/**
	 * @return all annotations reachable from the repository.
	 */
	Set<MergedAnnotation<Annotation>> getResolvedAnnotations();
}
