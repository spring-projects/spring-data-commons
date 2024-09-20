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

import static org.assertj.core.api.Assertions.assertThat;

import example.UserRepository;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.test.tools.ClassFile;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2024/09
 */
// testclass needs to be public otherwise we cannot reference the repository within
public class RepositoryBuilderUnitTests {

	@Test
	void compileInstance() {

		DummyAotRepoContext aotContext = new DummyAotRepoContext(UserRepository.class, null);
		RepositoryContributor repositoryContributor = new RepositoryContributor(aotContext) {

			@Override
			protected void customizeDerivedMethod(AotRepositoryMethodBuilder methodBuilder) {
				methodBuilder.customize(((repositoryInformation, metadata, builder) -> {
					if (!metadata.returnsVoid()) {
						builder.addStatement("return null");
					}
				}));
			}
		};

		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);
		repositoryContributor.contribute(generationContext);
		generationContext.writeGeneratedContent();

		TestCompiler.forSystem().with(generationContext).withClasses(aotContext.getRequiredContextFiles())
				.compile(compiled -> {
					assertThat(compiled.getAllCompiledClasses()).map(Class::getName).contains("example.UserRepositoryImpl__Aot");
				});
	}

	public static abstract class SimpleDummyRepository<T, ID> implements CrudRepository<T, ID> {}

	public static class DummyAotRepoContext implements AotRepositoryContext {

		private final StubRepositoryInformation repositoryInformation;

		public DummyAotRepoContext(Class<?> repositoryInterface, @Nullable RepositoryComposition composition) {
			this.repositoryInformation = new StubRepositoryInformation(repositoryInterface, composition);
		}

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
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

	public static class StubRepositoryInformation implements RepositoryInformation {

		private final RepositoryMetadata metadata;
		private final RepositoryComposition baseComposition;

		public StubRepositoryInformation(Class<?> repositoryInterface, @Nullable RepositoryComposition composition) {

			this.metadata = AbstractRepositoryMetadata.getMetadata(repositoryInterface);
			this.baseComposition = composition != null ? composition
					: RepositoryComposition.of(RepositoryFragment.structural(SimpleDummyRepository.class));
		}

		@Override
		public TypeInformation<?> getIdTypeInformation() {
			return metadata.getIdTypeInformation();
		}

		@Override
		public TypeInformation<?> getDomainTypeInformation() {
			return metadata.getDomainTypeInformation();
		}

		@Override
		public Class<?> getRepositoryInterface() {
			return metadata.getRepositoryInterface();
		}

		@Override
		public TypeInformation<?> getReturnType(Method method) {
			return metadata.getReturnType(method);
		}

		@Override
		public Class<?> getReturnedDomainClass(Method method) {
			return metadata.getReturnedDomainClass(method);
		}

		@Override
		public CrudMethods getCrudMethods() {
			return metadata.getCrudMethods();
		}

		@Override
		public boolean isPagingRepository() {
			return false;
		}

		@Override
		public Set<Class<?>> getAlternativeDomainTypes() {
			return null;
		}

		@Override
		public boolean isReactiveRepository() {
			return false;
		}

		@Override
		public Set<RepositoryFragment<?>> getFragments() {
			return null;
		}

		@Override
		public boolean isBaseClassMethod(Method method) {

			return baseComposition.findMethod(method).isPresent();
		}

		@Override
		public boolean isCustomMethod(Method method) {
			return false;
		}

		@Override
		public boolean isQueryMethod(Method method) {
			return false;
		}

		@Override
		public Streamable<Method> getQueryMethods() {
			return null;
		}

		@Override
		public Class<?> getRepositoryBaseClass() {
			return SimpleDummyRepository.class;
		}

		@Override
		public Method getTargetClassMethod(Method method) {
			return null;
		}

	}
}
