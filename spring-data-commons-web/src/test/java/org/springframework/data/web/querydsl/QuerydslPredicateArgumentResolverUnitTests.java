/*
 * Copyright 2015-2025 the original author or authors.
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
package org.springframework.data.web.querydsl;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolverSupport.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

import com.querydsl.core.types.Predicate;

/**
 * Unit tests for {@link QuerydslPredicateArgumentResolver}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class QuerydslPredicateArgumentResolverUnitTests {

	QuerydslPredicateArgumentResolver resolver;
	MockHttpServletRequest request;

	@BeforeEach
	void setUp() {

		resolver = new QuerydslPredicateArgumentResolver(new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE),
				Optional.empty());
		request = new MockHttpServletRequest();
	}

	@Test // DATACMNS-669
	void supportsParameterReturnsTrueWhenMethodParameterIsPredicateAndAnnotatedCorrectly() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("simpleFind", Predicate.class))).isTrue();
	}

	@Test // DATACMNS-669
	void supportsParameterReturnsTrueWhenMethodParameterIsPredicateButNotAnnotatedAsSuch() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("predicateWithoutAnnotation", Predicate.class)))
				.isTrue();
	}

	@Test // DATACMNS-669
	void supportsParameterShouldThrowExceptionWhenMethodParameterIsNoPredicateButAnnotatedAsSuch() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> resolver.supportsParameter(getMethodParameterFor("nonPredicateWithAnnotation", String.class)));
	}

	@Test // DATACMNS-669
	void supportsParameterReturnsFalseWhenMethodParameterIsNoPredicate() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("nonPredicateWithoutAnnotation", String.class)))
				.isFalse();
	}

	@Test // DATACMNS-669
	void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() throws Exception {

		request.addParameter("firstname", "rand");

		var predicate = resolver.resolveArgument(getMethodParameterFor("simpleFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("rand"));
	}

	@Test // DATACMNS-669
	void resolveArgumentShouldCreateMultipleParametersPredicateCorrectly() throws Exception {

		request.addParameter("firstname", "rand");
		request.addParameter("lastname", "al'thor");

		var predicate = resolver.resolveArgument(getMethodParameterFor("simpleFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("rand").and(QUser.user.lastname.eq("al'thor")));
	}

	@Test // DATACMNS-669
	void resolveArgumentShouldCreateNestedObjectPredicateCorrectly() throws Exception {

		request.addParameter("address.city", "two rivers");

		var predicate = resolver.resolveArgument(getMethodParameterFor("simpleFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		var eq = QUser.user.address.city.eq("two rivers");

		assertThat(predicate).isEqualTo(eq);
	}

	@Test // DATACMNS-669
	void resolveArgumentShouldResolveTypePropertyFromPageCorrectly() throws Exception {

		request.addParameter("address.city", "tar valon");

		var predicate = resolver.resolveArgument(getMethodParameterFor("pagedFind", Predicate.class, Pageable.class),
				null, new ServletWebRequest(request), null);

		assertThat(predicate).isEqualTo(QUser.user.address.city.eq("tar valon"));
	}

	@Test // DATACMNS-669
	void resolveArgumentShouldHonorCustomSpecification() throws Exception {

		request.addParameter("firstname", "egwene");
		request.addParameter("lastname", "al'vere");

		var predicate = resolver.resolveArgument(getMethodParameterFor("specificFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		assertThat(predicate).isEqualTo(
				QUser.user.firstname.eq("egwene".toUpperCase()).and(QUser.user.lastname.toLowerCase().eq("al'vere")));
	}

	@Test // #2277
	void resolveArgumentShouldHonorMetaAnnotation() throws Exception {

		request.addParameter("firstname", "egwene");
		request.addParameter("lastname", "al'vere");

		var predicate = resolver.resolveArgument(
				getMethodParameterFor("specificFindWithMetaAnnotation", Predicate.class), null, new ServletWebRequest(request),
				null);

		assertThat(predicate).isEqualTo(
				QUser.user.firstname.eq("egwene".toUpperCase()).and(QUser.user.lastname.toLowerCase().eq("al'vere")));
	}

	@Test // DATACMNS-669
	void shouldCreatePredicateForNonStringPropertyCorrectly() throws Exception {

		request.addParameter("inceptionYear", "978");

		var predicate = resolver.resolveArgument(getMethodParameterFor("specificFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		assertThat(predicate).isEqualTo(QUser.user.inceptionYear.eq(978L));
	}

	@Test // DATACMNS-669
	void shouldCreatePredicateForNonStringListPropertyCorrectly() throws Exception {

		request.addParameter("inceptionYear", new String[] { "978", "998" });

		var predicate = resolver.resolveArgument(getMethodParameterFor("specificFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		assertThat(predicate).isEqualTo(QUser.user.inceptionYear.in(978L, 998L));
	}

	@Test // DATACMNS-669
	void shouldExcludePropertiesCorrectly() throws Exception {

		request.addParameter("address.street", "downhill");
		request.addParameter("inceptionYear", "973");

		var predicate = resolver.resolveArgument(getMethodParameterFor("specificFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		assertThat(predicate.toString()).isEqualTo(QUser.user.inceptionYear.eq(973L).toString());
	}

	@Test // DATACMNS-669
	@SuppressWarnings("rawtypes")
	void extractTypeInformationShouldUseTypeExtractedFromMethodReturnTypeIfPredicateNotAnnotated() {

		TypeInformation<?> type = ReflectionTestUtils.invokeMethod(resolver, "extractTypeInfo",
				getMethodParameterFor("predicateWithoutAnnotation", Predicate.class), MergedAnnotation.missing());

		assertThat(type).isEqualTo(TypeInformation.of(User.class));
	}

	@Test // DATACMNS-669
	@SuppressWarnings("rawtypes")
	void detectsDomainTypesCorrectly() {

		TypeInformation USER_TYPE = TypeInformation.of(User.class);
		TypeInformation MODELA_AND_VIEW_TYPE = TypeInformation.of(ModelAndView.class);

		assertThat(extractTypeInfo(getMethodParameterFor("forEntity"), MergedAnnotation.missing())).isEqualTo(USER_TYPE);
		assertThat(extractTypeInfo(getMethodParameterFor("forResourceOfUser"), MergedAnnotation.missing()))
				.isEqualTo(USER_TYPE);
		assertThat(extractTypeInfo(getMethodParameterFor("forModelAndView"), MergedAnnotation.missing()))
				.isEqualTo(MODELA_AND_VIEW_TYPE);
	}

	@Test // DATACMNS-1593
	void returnsEmptyPredicateForEmptyInput() throws Exception {

		var parameter = getMethodParameterFor("predicateWithoutAnnotation", Predicate.class);

		request.addParameter("firstname", "");

		assertThat(resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null)) //
				.isNotNull();
	}

	@Test // DATACMNS-1635
	void forwardsNullValueForNullablePredicate() throws Exception {

		var parameter = getMethodParameterFor("nullablePredicateWithoutAnnotation", Predicate.class);

		request.addParameter("firstname", "");

		assertThat(resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null)).isNull();
	}

	@Test // DATACMNS-1635
	void returnsOptionalIfDeclared() throws Exception {

		var parameter = getMethodParameterFor("optionalPredicateWithoutAnnotation", Optional.class);

		request.addParameter("firstname", "");

		assertThat(resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null)) //
				.isInstanceOfSatisfying(Optional.class, it -> assertThat(it).isEmpty());

		request.addParameter("lastname", "Matthews");

		assertThat(resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null)) //
				.isInstanceOfSatisfying(Optional.class, it -> assertThat(it).isPresent());
	}

	private static MethodParameter getMethodParameterFor(String methodName, Class<?>... args) throws RuntimeException {

		try {
			return new MethodParameter(Sample.class.getMethod(methodName, args), args.length == 0 ? -1 : 0);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	static class SpecificBinding implements QuerydslBinderCustomizer<QUser> {

		@Override
		public void customize(QuerydslBindings bindings, QUser user) {

			bindings.bind(user.firstname).firstOptional((path, value) -> value.map(it -> path.eq(it.toUpperCase())));
			bindings.bind(user.lastname).first((path, value) -> path.toLowerCase().eq(value));

			bindings.excluding(user.address);
		}
	}

	static interface Sample {

		User predicateWithoutAnnotation(Predicate predicate);

		User nonPredicateWithAnnotation(@QuerydslPredicate String predicate);

		User nonPredicateWithoutAnnotation(String predicate);

		User simpleFind(@QuerydslPredicate Predicate predicate);

		Page<User> pagedFind(@QuerydslPredicate Predicate predicate, Pageable page);

		User specificFind(@QuerydslPredicate(bindings = SpecificBinding.class) Predicate predicate);

		User specificFindWithMetaAnnotation(@MyQuerydslPredicate Predicate predicate);

		HttpEntity<User> forEntity();

		ModelAndView forModelAndView();

		ResponseEntity<EntityModel<User>> forResourceOfUser();

		// Nullability

		User nullablePredicateWithoutAnnotation(@Nullable Predicate predicate);

		User optionalPredicateWithoutAnnotation(Optional<Predicate> predicate);
	}

	static class SampleRepo implements QuerydslBinderCustomizer<QUser> {

		@Override
		public void customize(QuerydslBindings bindings, QUser user) {
			bindings.bind(QUser.user.firstname).first((path, value) -> path.contains(value));
		}
	}

	@Target({ ElementType.PARAMETER, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@QuerydslPredicate
	public @interface MyQuerydslPredicate {

		/**
		 * To customize the way individual properties' values should be bound to the predicate a
		 * {@link QuerydslBinderCustomizer} can be specified here. We'll try to obtain a Spring bean of this type but fall
		 * back to a plain instantiation if no bean is found in the current
		 * {@link org.springframework.beans.factory.BeanFactory}.
		 *
		 * @return
		 */
		@SuppressWarnings("rawtypes")
		@AliasFor(annotation = QuerydslPredicate.class, attribute = "bindings")
		Class<? extends QuerydslBinderCustomizer> bindings() default SpecificBinding.class;
	}

}
