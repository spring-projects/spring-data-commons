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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import example.UserRepository.User;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.springframework.aot.generate.Generated;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Range;
import org.springframework.data.geo.Metric;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.config.AotRepositoryInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.AnnotationRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.DefaultParameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;
import org.springframework.stereotype.Repository;
import org.springframework.util.ClassUtils;

/**
 * Unit tests for {@link AotRepositoryCreator}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class AotRepositoryCreatorUnitTests {

	RepositoryInformation repositoryInformation;

	@BeforeEach
	void beforeEach() {

		repositoryInformation = mock(RepositoryInformation.class);
		doReturn(UserRepository.class).when(repositoryInformation).getRepositoryInterface();
	}

	@Test // GH-3279
	void writesClassSkeleton() {

		AotRepositoryCreator repositoryCreator = AotRepositoryCreator.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());

		// same package as source repo
		assertThat(generate(repositoryCreator)).contains("package %s;".formatted(UserRepository.class.getPackageName()))
				.contains("@Generated") // marked as generated source
				.contains("public class %sImpl".formatted(UserRepository.class.getSimpleName())) // target name
				.contains("public UserRepositoryImpl"); // default constructor if not arguments to wire
	}

	@Test // GH-3279
	void appliesCtorArguments() {

		AotRepositoryCreator repositoryCreator = AotRepositoryCreator.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());
		repositoryCreator.customizeConstructor(ctor -> {
			ctor.addParameter("param1", Metric.class);
			ctor.addParameter("param2", String.class);
			ctor.addParameter("ctorScoped", ResolvableType.forType(Object.class), false);
		});

		TypeSpec.Builder builder = TypeSpec
				.classBuilder(ClassName.get(UserRepository.class.getPackageName(), "UserRepositoryImpl"));

		assertThat(generate(builder, repositoryCreator)) //
				.contains("private final Metric param1;") //
				.contains("private final String param2;") //
				.doesNotContain("private final Object ctorScoped;") //
				.contains("public UserRepositoryImpl(Metric param1, String param2, Object ctorScoped)") //
				.contains("this.param1 = param1") //
				.contains("this.param2 = param2") //
				.doesNotContain("this.ctorScoped = ctorScoped");
	}

	@Test // GH-3279
	void appliesCtorCodeBlock() {

		AotRepositoryCreator repositoryCreator = AotRepositoryCreator.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());
		repositoryCreator.customizeConstructor(ctor -> {
			ctor.customize((code) -> {
				code.addStatement("throw new $T($S)", IllegalStateException.class, "initialization error");
			});
		});
		assertThat(generate(repositoryCreator)).containsIgnoringWhitespaces(
				"UserRepositoryImpl() { throw new IllegalStateException(\"initialization error\"); }");
	}

	@Test // GH-3279
	void appliesClassCustomizations() {

		AotRepositoryCreator repositoryCreator = AotRepositoryCreator.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());

		repositoryCreator.customizeClass((builder) -> {

			builder.customize(clazz -> {

				clazz.addField(Float.class, "f", Modifier.PRIVATE, Modifier.STATIC);
				clazz.addField(Double.class, "d", Modifier.PUBLIC);
				clazz.addField(TimeZone.class, "t", Modifier.FINAL);

				clazz.addAnnotation(Repository.class);

				clazz.addMethod(MethodSpec.methodBuilder("oops").build());
			});
		});

		assertThat(generate(repositoryCreator)) //
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

		AotRepositoryCreator repositoryCreator = AotRepositoryCreator.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());

		repositoryCreator.contributeMethods((method) -> {

			return new MethodContributor<>(mock(QueryMethod.class, Answers.RETURNS_MOCKS), null) {

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

		assertThat(generate(repositoryCreator)) //
				.containsIgnoringWhitespaces("void oops() { }");
	}

	@Test // GH-3279
	void shouldContributeFragmentImplementationMetadata() {

		AotRepositoryInformation repositoryInformation = new AotRepositoryInformation(
				AnnotationRepositoryMetadata.getMetadata(QuerydslUserRepository.class), CrudRepository.class,
				List.of(RepositoryFragment.structural(QuerydslPredicateExecutor.class, DummyQuerydslPredicateExecutor.class)));

		AotRepositoryCreator creator = AotRepositoryCreator.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());
		creator.contributeMethods(method -> null);
		AotRepositoryCreator.AotBundle bundle = doCreate(creator);

		AotRepositoryMethod method = bundle.metadata().get().methods().stream().filter(it -> it.name().equals("findBy"))
				.findFirst().get();

		assertThat(method.fragment()).isNotNull();
		assertThat(method.fragment().signature()).isEqualTo(QuerydslPredicateExecutor.class.getName());
		assertThat(method.fragment().implementation()).isEqualTo(DummyQuerydslPredicateExecutor.class.getName());
	}

	@Test // GH-3339
	void usesTargetTypeName() {

		AotRepositoryCreator repositoryCreator = AotRepositoryCreator.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());
		repositoryCreator.customizeConstructor(ctor -> {
			ctor.addParameter("param1", Metric.class);
			ctor.addParameter("param2", String.class);
			ctor.addParameter("ctorScoped", ResolvableType.forType(Object.class), false);
		});

		TypeReference targetType = TypeReference.of("%s__AotPostfix".formatted(UserRepository.class.getCanonicalName()));

		assertThat(generate(ClassName.get(repositoryCreator.packageName(), targetType.getSimpleName()), repositoryCreator)) //
				.contains("class %s".formatted(targetType.getSimpleName())) //
				.contains("public %s(Metric param1, String param2, Object ctorScoped)".formatted(targetType.getSimpleName()));
	}

	@Test // GH-3339
	void usesGenericConstructorArguments() {

		AotRepositoryCreator repositoryCreator = AotRepositoryCreator.forRepository(repositoryInformation, "Commons",
				new SpelAwareProxyProjectionFactory());
		repositoryCreator.customizeConstructor(ctor -> {
			ctor.addParameter("param1", ResolvableType.forClassWithGenerics(List.class, Metric.class));
			ctor.addParameter("param2", String.class);
			ctor.addParameter("ctorScoped", ResolvableType.NONE, false);
		});

		TypeReference targetType = TypeReference.of("%s__AotPostfix".formatted(UserRepository.class.getCanonicalName()));

		assertThat(generate(ClassName.get(repositoryCreator.packageName(), targetType.getSimpleName()), repositoryCreator)) //
				.contains("class %s".formatted(targetType.getSimpleName())) //
				.contains(
						"public %s(List<Metric> param1, String param2, Object ctorScoped)".formatted(targetType.getSimpleName()));
	}

	@Test // GH-3374
	void skipsMethodWithUnresolvableGenericReturnType() {

		SpelAwareProxyProjectionFactory spelAwareProxyProjectionFactory = new SpelAwareProxyProjectionFactory();
		AotRepositoryInformation repositoryInformation = new AotRepositoryInformation(
				AnnotationRepositoryMetadata.getMetadata(UserRepository.class), CrudRepository.class,
				List.of(RepositoryFragment.structural(QuerydslPredicateExecutor.class, DummyQuerydslPredicateExecutor.class)));

		AotRepositoryCreator repositoryCreator = AotRepositoryCreator.forRepository(repositoryInformation, "Commons",
				spelAwareProxyProjectionFactory);
		repositoryCreator.contributeMethods(method -> {

			QueryMethod queryMethod = new QueryMethod(method, repositoryInformation, spelAwareProxyProjectionFactory,
					DefaultParameters::new);

			return MethodContributor.forQueryMethod(queryMethod).withMetadata(Map::of).contribute(context -> {

				CodeBlock.Builder builder = CodeBlock.builder();
				if (!ClassUtils.isVoidType(method.getReturnType())) {
					builder.addStatement("return null");
				}

				return builder.build();
			});
		});

		// same package as source repo
		String generated = generate(repositoryCreator);

		assertThat(generated) //
				// very simple ones with fixed type signature
				.contains("findByFirstname") //
				.contains("rangeQuery") //
				.contains("public String someMethod()") //
				.contains("public String findByPrimitive(int value)") //
				.contains("public String findByPrimitiveArray(int[] values)") //
				.containsSubsequence("public", "User typedInputParameter(", "User value)") //
				.containsSubsequence("List<", "User> findByFirstnameIn(List<String> firstname)") //

				// <T> List<T> findByFirstname(String firstname, Class<T> type);
				.contains("public <T> List findByFirstname(String firstname, Class type)")

				// <T extends User> List<T> project1ByFirstname(String firstname, Class type);
				.contains("public <T> List project2ByFirstname(String firstname, Class type)")

				// <T extends User> List<T> project2ByFirstname(String firstname, Class<? extends T> type)
				.contains("public <T> List project2ByFirstname(String firstname, Class type)")

				// List<User> geoQuery(GeoJson<?> geoJson);
				.containsSubsequence("public List<", "User> geoQuery(", "GeoJson geoJson)")

				// <T> List<T> genInputType(String firstname, T value);
				.contains("public <T> List genInputType(String firstname, Object value)")

				.doesNotContain("baseProjection") //
				.doesNotContain("upperBoundedProjection") //
				.doesNotContain("lowerBoundedProjection()");
	}

	static Stream<Method> declaredUserRepositoryMethods() {
		return Arrays.stream(UserRepository.class.getDeclaredMethods());
	}

	static Stream<Method> unresolvedRepositoryMethods() {
		return Arrays.stream(UserRepository.class.getMethods())
				.filter(it -> it.getDeclaringClass().equals(BaseRepository.class))
				.filter(it -> it.getName().startsWith("upper") || it.getName().startsWith("lower"));
	}

	static Stream<Method> resolvedRepositoryMethods() {
		return Arrays.stream(UserRepository.class.getMethods())
				.filter(it -> it.getDeclaringClass().equals(BaseRepository.class))
				.filter(it -> it.getName().startsWith("parametrized"));
	}

	@ParameterizedTest
	@MethodSource("declaredUserRepositoryMethods")
	void shouldResolveGenerics(Method method) {

		assertThat(ResolvableGenerics.of(method, UserRepository.class).hasUnresolvableGenerics()).isFalse();
	}

	@ParameterizedTest
	@MethodSource("resolvedRepositoryMethods")
	void shouldResolveInterfaceGenerics(Method method) {

		assertThat(ResolvableGenerics.of(method, UserRepository.class).hasUnresolvableGenerics()).isFalse();
	}

	@ParameterizedTest
	@MethodSource("unresolvedRepositoryMethods")
	void shouldReportUnresolvedGenerics(Method method) {

		assumeThat(method.getDeclaringClass()).isEqualTo(BaseRepository.class);

		assertThat(ResolvableGenerics.of(method, UserRepository.class).hasUnresolvableGenerics()).isTrue();
	}

	private AotRepositoryCreator.AotBundle doCreate(AotRepositoryCreator creator) {
		return creator.create(getTypeSpecBuilder(creator));
	}

	private String generate(AotRepositoryCreator creator) {

		TypeSpec.Builder builder = getTypeSpecBuilder(creator);
		return generate(builder, creator);
	}

	private String generate(ClassName className, AotRepositoryCreator creator) {

		TypeSpec.Builder builder = getTypeSpecBuilder(className);
		return generate(builder, creator);
	}

	private String generate(TypeSpec.Builder builder, AotRepositoryCreator creator) {

		creator.create(builder);

		return JavaFile.builder(creator.packageName(), builder.build()).build().toString();
	}

	private static TypeSpec.Builder getTypeSpecBuilder(AotRepositoryCreator creator) {
		return getTypeSpecBuilder(creator.getClassName());
	}

	private static TypeSpec.Builder getTypeSpecBuilder(ClassName className) {
		return TypeSpec.classBuilder(className).addAnnotation(Generated.class);
	}

	interface BaseRepository<T, ID> extends org.springframework.data.repository.Repository<T, ID> {

		<P extends T> List<P> upperBoundedProjection(String firstname, Class<T> type);

		List<? super T> lowerBoundedProjection(String firstname, Class<T> type);

		List<T> parametrizedListProjection(String firstname, Class<T> type);

		T parametrizedSelection(String firstname);

		T typedInputParameter(T value);

	}

	interface UserRepository extends BaseRepository<User, String> {

		String someMethod();

		List<User> findByFirstnameIn(List<String> firstname);

		String findByPrimitive(int value);

		String findByPrimitiveArray(int[] values);

		<T> List<T> findByFirstname(String firstname, Class<T> type);

		<T> List<T> genInputType(String firstname, T value);

		<T extends User> List<T> project1ByFirstname(String firstname, Class<? super T> type);

		<T extends User> List<T> project2ByFirstname(String firstname, Class<? extends T> type);

		List<User> geoQuery(GeoJson<?> geoJson);

		List<User> rangeQuery(Range<?> geoJson);

	}

	public interface GeoJson<T extends Iterable<?>> {}

	interface QuerydslUserRepository
			extends org.springframework.data.repository.Repository<User, String>, QuerydslPredicateExecutor<User> {

	}

	interface DummyQuerydslPredicateExecutor<T> extends QuerydslPredicateExecutor<T> {}
}
