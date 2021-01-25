/*
 * Copyright 2015-2021 the original author or authors.
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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.querydsl.core.types.Predicate;

/**
 * Unit tests for {@link ReactiveQuerydslPredicateArgumentResolver}.
 *
 * @author Mark Paluch
 */
class ReactiveQuerydslPredicateArgumentResolverUnitTests {

	ReactiveQuerydslPredicateArgumentResolver resolver;

	@BeforeEach
	void setUp() {

		resolver = new ReactiveQuerydslPredicateArgumentResolver(
				new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE), DefaultConversionService.getSharedInstance());
	}

	@Test // DATACMNS-1785
	void supportsParameterReturnsTrueWhenMethodParameterIsPredicateButNotAnnotatedAsSuch() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("predicateWithoutAnnotation", Predicate.class)))
				.isTrue();
	}

	@Test // DATACMNS-1785
	void supportsParameterShouldThrowExceptionWhenMethodParameterIsNoPredicateButAnnotatedAsSuch() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> resolver.supportsParameter(getMethodParameterFor("nonPredicateWithAnnotation", String.class)));
	}

	@Test // DATACMNS-1785
	void supportsParameterReturnsFalseWhenMethodParameterIsNoPredicate() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("nonPredicateWithoutAnnotation", String.class)))
				.isFalse();
	}

	@Test // DATACMNS-1785
	void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() {

		MockServerHttpRequest request = MockServerHttpRequest.get("").queryParam("firstname", "rand").build();

		Object predicate = resolver.resolveArgumentValue(getMethodParameterFor("simpleFind", Predicate.class), null,
				MockServerWebExchange.from(request));

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("rand"));
	}

	@Test // DATACMNS-1785
	void resolveArgumentShouldHonorCustomSpecification() {

		MockServerHttpRequest request = MockServerHttpRequest.get("").queryParam("firstname", "egwene")
				.queryParam("lastname", "al'vere").build();

		Object predicate = resolver.resolveArgumentValue(getMethodParameterFor("specificFind", Predicate.class), null,
				MockServerWebExchange.from(request));

		assertThat(predicate).isEqualTo(
				QUser.user.firstname.eq("egwene".toUpperCase()).and(QUser.user.lastname.toLowerCase().eq("al'vere")));
	}

	@Test // DATACMNS-1785
	void returnsEmptyPredicateForEmptyInput() {

		MethodParameter parameter = getMethodParameterFor("predicateWithoutAnnotation", Predicate.class);

		MockServerHttpRequest request = MockServerHttpRequest.get("").queryParam("firstname", "").build();

		assertThat(resolver.resolveArgumentValue(parameter, null, MockServerWebExchange.from(request))) //
				.isNotNull();
	}

	@Test // DATACMNS-1785
	void forwardsNullValueForNullablePredicate() {

		MethodParameter parameter = getMethodParameterFor("nullablePredicateWithoutAnnotation", Predicate.class);

		MockServerHttpRequest request = MockServerHttpRequest.get("").queryParam("firstname", "").build();

		assertThat(resolver.resolveArgumentValue(parameter, null, MockServerWebExchange.from(request))).isNull();
	}

	@Test // DATACMNS-1785
	void returnsOptionalIfDeclared() {

		MethodParameter parameter = getMethodParameterFor("optionalPredicateWithoutAnnotation", Optional.class);

		MockServerHttpRequest request = MockServerHttpRequest.get("").queryParam("firstname", "").build();

		assertThat(resolver.resolveArgumentValue(parameter, null, MockServerWebExchange.from(request))) //
				.isInstanceOfSatisfying(Optional.class, it -> assertThat(it).isEmpty());

		request = MockServerHttpRequest.get("").queryParam("firstname", "Matthews").build();

		assertThat(resolver.resolveArgumentValue(parameter, null, MockServerWebExchange.from(request))) //
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

		public void customize(QuerydslBindings bindings, QUser user) {

			bindings.bind(user.firstname).firstOptional((path, value) -> value.map(it -> path.eq(it.toUpperCase())));
			bindings.bind(user.lastname).first((path, value) -> path.toLowerCase().eq(value));

			bindings.excluding(user.address);
		}
	}

	interface Sample {

		User predicateWithoutAnnotation(Predicate predicate);

		User nonPredicateWithAnnotation(@QuerydslPredicate String predicate);

		User nonPredicateWithoutAnnotation(String predicate);

		User simpleFind(@QuerydslPredicate Predicate predicate);

		User specificFind(@QuerydslPredicate(bindings = SpecificBinding.class) Predicate predicate);

		// Nullability

		User nullablePredicateWithoutAnnotation(@Nullable Predicate predicate);

		User optionalPredicateWithoutAnnotation(Optional<Predicate> predicate);
	}

}
