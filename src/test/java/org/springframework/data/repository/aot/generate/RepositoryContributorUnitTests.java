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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import example.UserRepository;
import example.UserRepositoryExtension;
import example.UserRepositoryExtensionImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.core.test.tools.ResourceFile;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.data.aot.CodeContributionAssert;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.DefaultParameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ClassUtils;

/**
 * Unit tests targeting {@link RepositoryContributor}.
 *
 * @author Christoph Strobl
 */
class RepositoryContributorUnitTests {

	@Test // GH-3279
	void createsCompilableClassStub() {

		DummyModuleAotRepositoryContext aotContext = new DummyModuleAotRepositoryContext(UserRepository.class, null);
		RepositoryContributor repositoryContributor = new RepositoryContributor(aotContext) {

			@Override
			protected @Nullable MethodContributor<? extends QueryMethod> contributeQueryMethod(Method method) {

				return MethodContributor
						.forQueryMethod(
								new QueryMethod(method, getRepositoryInformation(), getProjectionFactory(), DefaultParameters::new))
						.withMetadata(new QueryMetadata() {

							@Override
							public Map<String, Object> serialize() {
								return Map.of();
							}
						}).contribute(context -> {

							CodeBlock.Builder builder = CodeBlock.builder();
							if (!ClassUtils.isVoidType(method.getReturnType())) {
								builder.addStatement("return null");
							}

							return builder.build();
						});
			}
		};

		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);
		repositoryContributor.contribute(generationContext);
		generationContext.writeGeneratedContent();

		String expectedTypeName = "example.UserRepositoryImpl__AotRepository";

		TestCompiler.forSystem().with(generationContext).compile(compiled -> {
			assertThat(compiled.getAllCompiledClasses()).map(Class::getName).contains(expectedTypeName);
		});

		new CodeContributionAssert(generationContext).contributesReflectionFor(expectedTypeName);
	}

	@Test // GH-3265
	void writesCapturedQueryMetadataToResources() {

		DummyModuleAotRepositoryContext aotContext = new DummyModuleAotRepositoryContext(UserRepository.class, null);
		RepositoryContributor repositoryContributor = new RepositoryContributor(aotContext) {

			@Override
			protected @Nullable MethodContributor<? extends QueryMethod> contributeQueryMethod(Method method) {

				return MethodContributor
						.forQueryMethod(
								new QueryMethod(method, getRepositoryInformation(), getProjectionFactory(), DefaultParameters::new))
						.withMetadata(() -> Map.of("filter", "FILTER(%s > $1)".formatted(method.getName()), "project",
								Arrays.stream(method.getParameters()).map(Parameter::getName).toList()))
						.contribute(context -> {

							CodeBlock.Builder builder = CodeBlock.builder();
							if (!ClassUtils.isVoidType(method.getReturnType())) {
								builder.addStatement("return null");
							}

							return builder.build();
						});
			}
		};

		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);
		repositoryContributor.contribute(generationContext);
		generationContext.writeGeneratedContent();

		String expectedTypeName = "example.UserRepositoryImpl__AotRepository";

		TestCompiler.forSystem().with(generationContext).compile(compiled -> {
			String content = compiled.getResourceFile().getContent();
			assertThat(content).containsIgnoringWhitespaces("\"filter\": \"FILTER(doSomething > $1)\"")
					.containsIgnoringWhitespaces("\"project\": [\n\"firstname\"\n]");
		});

		new CodeContributionAssert(generationContext).contributesReflectionFor(expectedTypeName);
	}

	@Test // GH-3354
	void doesNotWriteCapturedQueryMetadataToResourcesIfDisabled() {

		DummyModuleAotRepositoryContext aotContext = new DummyModuleAotRepositoryContext(UserRepository.class, null);
		aotContext.getEnvironment().setProperty("spring.aot.repositories.metadata.enabled", "false");

		RepositoryContributor repositoryContributor = new RepositoryContributor(aotContext) {

			@Override
			protected @Nullable MethodContributor<? extends QueryMethod> contributeQueryMethod(Method method) {

				return MethodContributor
						.forQueryMethod(
								new QueryMethod(method, getRepositoryInformation(), getProjectionFactory(), DefaultParameters::new))
						.withMetadata(() -> Map.of("filter", "FILTER(%s > $1)".formatted(method.getName()), "project",
								Arrays.stream(method.getParameters()).map(Parameter::getName).toList()))
						.contribute(context -> {

							CodeBlock.Builder builder = CodeBlock.builder();
							if (!ClassUtils.isVoidType(method.getReturnType())) {
								builder.addStatement("return null");
							}

							return builder.build();
						});
			}
		};

		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);
		repositoryContributor.contribute(generationContext);
		generationContext.writeGeneratedContent();

		TestCompiler.forSystem().with(generationContext).compile(compiled -> {
			assertThat(compiled.getResourceFiles()).isEmpty();
		});
	}

	@Test // GH-3354
	void doesNotWriteCapturedQueryMetadataToResourcesIfAlreadyExists() {

		DummyModuleAotRepositoryContext aotContext = new DummyModuleAotRepositoryContext(UserRepository.class, null);

		RepositoryContributor repositoryContributor = new RepositoryContributor(aotContext) {

			@Override
			protected @Nullable MethodContributor<? extends QueryMethod> contributeQueryMethod(Method method) {

				return MethodContributor
						.forQueryMethod(
								new QueryMethod(method, getRepositoryInformation(), getProjectionFactory(), DefaultParameters::new))
						.withMetadata(() -> Map.of("filter", "FILTER(%s > $1)".formatted(method.getName()), "project",
								Arrays.stream(method.getParameters()).map(Parameter::getName).toList()))
						.contribute(context -> {

							CodeBlock.Builder builder = CodeBlock.builder();
							if (!ClassUtils.isVoidType(method.getReturnType())) {
								builder.addStatement("return null");
							}

							return builder.build();
						});
			}
		};

		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);
		repositoryContributor.contribute(generationContext);
		generationContext.writeGeneratedContent();

		ResourceFile rf = ResourceFile.of(UserRepository.class.getName().replace('.', '/') + ".json",
				"But you're untouchable, burning brighter than the sun");
		TestCompiler.forSystem().with(generationContext).withResources(rf).compile(compiled -> {
			String content = compiled.getResourceFile().getContent();
			assertThat(content).contains("you're untouchable").doesNotContain("FILTER(doSomething > $1)");
		});
	}

	@Test // GH-3279
	void callsMethodContributionForQueryMethod() {

		AotRepositoryContext repositoryContext = mock(AotRepositoryContext.class);
		RepositoryInformation repositoryInformation = mock(RepositoryInformation.class);

		when(repositoryContext.getRepositoryInformation()).thenReturn(repositoryInformation);
		when(repositoryInformation.getRepositoryInterface()).thenReturn((Class) UserRepository.class);
		when(repositoryInformation.isQueryMethod(argThat(it -> it.getName().equals("findByFirstname")))).thenReturn(true);

		MethodCapturingRepositoryContributor contributor = new MethodCapturingRepositoryContributor(repositoryContext);
		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);
		contributor.contribute(generationContext);
		generationContext.writeGeneratedContent();

		contributor.verifyContributionFor("findByFirstname");
	}

	@Test // GH-3279
	void doesNotContributeBaseClassMethods() {

		AotRepositoryContext repositoryContext = mock(AotRepositoryContext.class);
		when(repositoryContext.getModuleName()).thenReturn("Commons");
		RepositoryInformation repositoryInformation = mock(RepositoryInformation.class);

		when(repositoryContext.getRepositoryInformation()).thenReturn(repositoryInformation);
		when(repositoryInformation.getRepositoryInterface()).thenReturn((Class) UserRepository.class);
		when(repositoryInformation.getRepositoryComposition())
				.thenReturn(RepositoryComposition.of(RepositoryFragment.structural(RepoBaseClass.class)));
		when(repositoryInformation.isBaseClassMethod(argThat(it -> it.getName().equals("findByFirstname"))))
				.thenReturn(true);
		when(repositoryInformation.isQueryMethod(argThat(it -> !it.getName().equals("findByFirstname")))).thenReturn(true);

		TestGenerationContext testGenerationContext = new TestGenerationContext(UserRepository.class);
		MethodCapturingRepositoryContributor contributor = new MethodCapturingRepositoryContributor(repositoryContext);
		contributor.contribute(testGenerationContext);
		testGenerationContext.writeGeneratedContent();

		contributor.verifyContributedMethods().isNotEmpty().doesNotContainKey("findByFirstname");
	}

	@Test // GH-3279
	void doesNotContributeFragmentMethod() {

		AotRepositoryContext repositoryContext = mock(AotRepositoryContext.class);
		when(repositoryContext.getModuleName()).thenReturn("Commons");
		RepositoryInformation repositoryInformation = mock(RepositoryInformation.class);

		when(repositoryContext.getRepositoryInformation()).thenReturn(repositoryInformation);
		when(repositoryInformation.getRepositoryInterface()).thenReturn((Class) UserRepository.class);
		when(repositoryInformation.getRepositoryComposition())
				.thenReturn(RepositoryComposition.of(RepositoryFragment.structural(UserRepository.class))
						.append(RepositoryFragments
								.from(Set.of(new RepositoryFragment.ImplementedRepositoryFragment(UserRepositoryExtension.class,
										UserRepositoryExtensionImpl.class)))));

		when(repositoryInformation.isCustomMethod(argThat(it -> it.getName().equals("findUserByExtensionMethod"))))
				.thenReturn(true);
		when(repositoryInformation.isQueryMethod(argThat(it -> it.getName().equals("findByFirstname")))).thenReturn(true);

		MethodCapturingRepositoryContributor contributor = new MethodCapturingRepositoryContributor(repositoryContext);
		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);
		contributor.contribute(generationContext);
		generationContext.writeGeneratedContent();

		contributor.verifyContributedMethods().isNotEmpty().doesNotContainKey("findUserByExtensionMethod");
	}

	@Test // GH-3279
	void contributesBaseClassMethodIfQueryMethod() {

		AotRepositoryContext repositoryContext = mock(AotRepositoryContext.class);
		when(repositoryContext.getModuleName()).thenReturn("Commons");
		RepositoryInformation repositoryInformation = mock(RepositoryInformation.class);

		when(repositoryContext.getRepositoryInformation()).thenReturn(repositoryInformation);
		when(repositoryInformation.getRepositoryInterface()).thenReturn((Class) UserRepository.class);
		when(repositoryInformation.getRepositoryComposition())
				.thenReturn(RepositoryComposition.of(RepositoryFragment.structural(RepoBaseClass.class)));
		when(repositoryInformation.isBaseClassMethod(argThat(it -> it.getName().equals("findByFirstname"))))
				.thenReturn(true);
		when(repositoryInformation.isQueryMethod(any())).thenReturn(true);

		MethodCapturingRepositoryContributor contributor = new MethodCapturingRepositoryContributor(repositoryContext);
		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);
		contributor.contribute(generationContext);
		generationContext.writeGeneratedContent();

		contributor.verifyContributedMethods().containsKey("findByFirstname").hasSizeGreaterThan(1);
	}

	static class RepoBaseClass<T, ID> implements CrudRepository<T, ID> {

		private CrudRepository<T, ID> delegate;

		public <S extends T> S save(S entity) {
			return this.delegate.save(entity);
		}

		@Override
		public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
			return this.delegate.saveAll(entities);
		}

		public Optional<T> findById(ID id) {
			return this.delegate.findById(id);
		}

		@Override
		public boolean existsById(ID id) {
			return this.delegate.existsById(id);
		}

		@Override
		public Iterable<T> findAll() {
			return this.delegate.findAll();
		}

		@Override
		public Iterable<T> findAllById(Iterable<ID> ids) {
			return this.delegate.findAllById(ids);
		}

		@Override
		public long count() {
			return this.delegate.count();
		}

		@Override
		public void deleteById(ID id) {
			this.delegate.deleteById(id);
		}

		@Override
		public void delete(T entity) {
			this.delegate.delete(entity);
		}

		@Override
		public void deleteAllById(Iterable<? extends ID> ids) {
			this.delegate.deleteAllById(ids);
		}

		@Override
		public void deleteAll(Iterable<? extends T> entities) {
			this.delegate.deleteAll(entities);
		}

		@Override
		public void deleteAll() {
			this.delegate.deleteAll();
		}
	}
}
