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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import example.UserRepository.User;

import java.util.List;
import java.util.TimeZone;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.geo.Metric;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.config.AotRepositoryInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.AnnotationRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.stereotype.Repository;

/**
 * Unit tests for {@link AotRepositoryBuilder}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class AotRepositoryBuilderUnitTests {

	RepositoryInformation repositoryInformation;

	@BeforeEach
	void beforeEach() {

		repositoryInformation = mock(RepositoryInformation.class);
		doReturn(UserRepository.class).when(repositoryInformation).getRepositoryInterface();
	}

	@Test // GH-3279
	void writesClassSkeleton() {

		AotRepositoryBuilder repoBuilder = AotRepositoryBuilder.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());
		assertThat(repoBuilder.build().javaFile().toString())
				.contains("package %s;".formatted(UserRepository.class.getPackageName())) // same package as source repo
				.contains("@Generated") // marked as generated source
				.contains("public class %sImpl__Aot".formatted(UserRepository.class.getSimpleName())) // target name
				.contains("public UserRepositoryImpl__Aot()"); // default constructor if not arguments to wire
	}

	@Test // GH-3279
	void appliesCtorArguments() {

		AotRepositoryBuilder repoBuilder = AotRepositoryBuilder.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());
		repoBuilder.withConstructorCustomizer(ctor -> {
			ctor.addParameter("param1", Metric.class);
			ctor.addParameter("param2", String.class);
			ctor.addParameter("ctorScoped", TypeName.OBJECT, false);
		});
		assertThat(repoBuilder.build().javaFile().toString()) //
				.contains("private final Metric param1;") //
				.contains("private final String param2;") //
				.doesNotContain("private final Object ctorScoped;") //
				.contains("public UserRepositoryImpl__Aot(Metric param1, String param2, Object ctorScoped)") //
				.contains("this.param1 = param1") //
				.contains("this.param2 = param2") //
				.doesNotContain("this.ctorScoped = ctorScoped");
	}

	@Test // GH-3279
	void appliesCtorCodeBlock() {

		AotRepositoryBuilder repoBuilder = AotRepositoryBuilder.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());
		repoBuilder.withConstructorCustomizer(ctor -> {
			ctor.customize((code) -> {
				code.addStatement("throw new $T($S)", IllegalStateException.class, "initialization error");
			});
		});
		assertThat(repoBuilder.build().javaFile().toString()).containsIgnoringWhitespaces(
				"UserRepositoryImpl__Aot() { throw new IllegalStateException(\"initialization error\"); }");
	}

	@Test // GH-3279
	void appliesClassCustomizations() {

		AotRepositoryBuilder repoBuilder = AotRepositoryBuilder.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());

		repoBuilder.withClassCustomizer((builder) -> {

			builder.customize(clazz -> {

			clazz.addField(Float.class, "f", Modifier.PRIVATE, Modifier.STATIC);
			clazz.addField(Double.class, "d", Modifier.PUBLIC);
			clazz.addField(TimeZone.class, "t", Modifier.FINAL);

			clazz.addAnnotation(Repository.class);

			clazz.addMethod(MethodSpec.methodBuilder("oops").build());
			});
		});

		assertThat(repoBuilder.build().javaFile().toString()) //
				.contains("@Repository") //
				.contains("private static Float f;") //
				.contains("public Double d;") //
				.contains("final TimeZone t;") //
				.containsIgnoringWhitespaces("void oops() { }");
	}

	@Test // GH-3279
	void appliesQueryMethodContributor() {

		AotRepositoryInformation repositoryInformation = new AotRepositoryInformation(
				AnnotationRepositoryMetadata.getMetadata(UserRepository.class), CrudRepository.class, List.of());

		AotRepositoryBuilder repoBuilder = AotRepositoryBuilder.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());

		repoBuilder.withQueryMethodContributor((method) -> {

			return new MethodContributor<>(mock(QueryMethod.class), null) {

				@Override
				public MethodSpec contribute(AotQueryMethodGenerationContext context) {
					return MethodSpec.methodBuilder("oops").build();
				}

				@Override
				public boolean contributesMethodSpec() {
					return true;
				}
			};
		});

		assertThat(repoBuilder.build().javaFile().toString()) //
				.containsIgnoringWhitespaces("void oops() { }");
	}

	@Test // GH-3279
	void shouldContributeFragmentImplementationMetadata() {

		AotRepositoryInformation repositoryInformation = new AotRepositoryInformation(
				AnnotationRepositoryMetadata.getMetadata(QuerydslUserRepository.class), CrudRepository.class,
				List.of(RepositoryFragment.structural(QuerydslPredicateExecutor.class, DummyQuerydslPredicateExecutor.class)));

		AotRepositoryBuilder builder = AotRepositoryBuilder.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());
		AotRepositoryBuilder.AotBundle bundle = builder.build();

		AotRepositoryMethod method = bundle.metadata().methods().stream().filter(it -> it.name().equals("findBy"))
				.findFirst().get();

		assertThat(method.fragment()).isNotNull();
		assertThat(method.fragment().signature()).isEqualTo(QuerydslPredicateExecutor.class.getName());
		assertThat(method.fragment().implementation()).isEqualTo(DummyQuerydslPredicateExecutor.class.getName());
	}

	interface UserRepository extends org.springframework.data.repository.Repository<User, String> {

		String someMethod();
	}

	interface QuerydslUserRepository
			extends org.springframework.data.repository.Repository<User, String>, QuerydslPredicateExecutor<User> {

	}

	interface DummyQuerydslPredicateExecutor<T> extends QuerydslPredicateExecutor<T> {}
}
