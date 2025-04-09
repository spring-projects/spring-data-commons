/*
 * Copyright 2024 the original author or authors.
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.test.tools.ClassFile;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.lang.Nullable;

/**
 * Dummy {@link AotRepositoryContext} used to simulate module specific repository implementation.
 *
 * @author Christoph Strobl
 */
class DummyModuleAotRepositoryContext implements AotRepositoryContext {

	private final StubRepositoryInformation repositoryInformation;

	public DummyModuleAotRepositoryContext(Class<?> repositoryInterface, @Nullable RepositoryComposition composition) {
		this.repositoryInformation = new StubRepositoryInformation(repositoryInterface, composition);
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() {
		return null;
	}

	@Override
	public Environment getEnvironment() {
		return null;
	}

	@Override
	public TypeIntrospector introspectType(String typeName) {
		return null;
	}

	@Override
	public IntrospectedBeanDefinition introspectBeanDefinition(String beanName) {
		return null;
	}

	@Override
	public String getBeanName() {
		return "dummyRepository";
	}

	@Override
	public Set<String> getBasePackages() {
		return Set.of("org.springframework.data.dummy.repository.aot");
	}

	@Override
	public Set<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Set.of();
	}

	@Override
	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	@Override
	public Set<MergedAnnotation<Annotation>> getResolvedAnnotations() {
		return Set.of();
	}

	@Override
	public Set<Class<?>> getResolvedTypes() {
		return Set.of();
	}

	public List<ClassFile> getRequiredContextFiles() {
		return List.of(classFileForType(repositoryInformation.getRepositoryBaseClass()));
	}

	static ClassFile classFileForType(Class<?> type) {

		String name = type.getName();
		ClassPathResource cpr = new ClassPathResource(name.replaceAll("\\.", "/") + ".class");

		try {
			return ClassFile.of(name, cpr.getContentAsByteArray());
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot open [%s].".formatted(cpr.getPath()));
		}
	}
}
