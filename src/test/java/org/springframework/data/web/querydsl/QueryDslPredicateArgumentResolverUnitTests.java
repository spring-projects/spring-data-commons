/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.web.querydsl;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.User;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import com.mysema.query.types.PathImpl;
import com.mysema.query.types.PathMetadataFactory;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.PathBuilderFactory;

/**
 * @author Christoph Strobl
 */
public class QueryDslPredicateArgumentResolverUnitTests {

	QueryDslPredicateArgumentResolver resolver;
	MockHttpServletRequest request;

	@Before
	public void setUp() {

		resolver = new QueryDslPredicateArgumentResolver();
		request = new MockHttpServletRequest();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void supportsParameterReturnsTrueWhenMethodParameterIsPredicateAndAnnotatedCorrectly() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("simpleFind", Predicate.class)), is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void supportsParameterReturnsFalseWhenMethodParameterIsPredicateButNotAnnotatedAsSuch() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("predicateWithoutAnnotation", Predicate.class)),
				is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void supportsParameterReturnsFalseWhenMethodParameterIsNoPredicateButAnnotatedAsSuch() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("nonPredicateWithAnnotation", String.class)), is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() throws Exception {

		request.addParameter("firstname", "rand");

		Predicate predicate = (Predicate) resolver.resolveArgument(getMethodParameterFor("simpleFind", Predicate.class),
				null, new ServletWebRequest(request), null);

		assertThat(predicate.toString(), is("user.firstname = rand"));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateMultipleParametersPredicateCorrectly() throws Exception {

		request.addParameter("firstname", "rand");
		request.addParameter("lastname", "al'thor");

		Predicate predicate = (Predicate) resolver.resolveArgument(getMethodParameterFor("simpleFind", Predicate.class),
				null, new ServletWebRequest(request), null);

		assertThat(predicate.toString(), is("user.firstname = rand && user.lastname = al'thor"));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateNestedObjectPredicateCorrectly() throws Exception {

		request.addParameter("address.city", "two rivers");

		Predicate predicate = (Predicate) resolver.resolveArgument(getMethodParameterFor("simpleFind", Predicate.class),
				null, new ServletWebRequest(request), null);

		assertThat(predicate.toString(), is("user.address.city = two rivers"));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldResolveTypePropertyFromPageCorrectly() throws Exception {

		request.addParameter("address.city", "tar valon");

		Predicate predicate = (Predicate) resolver
				.resolveArgument(getMethodParameterFor("pagedFind", Predicate.class, Pageable.class), null,
						new ServletWebRequest(request), null);

		assertThat(predicate.toString(), is("user.address.city = tar valon"));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldHonorCustomSpeficifcation() throws Exception {

		request.addParameter("firstname", "egwene");
		request.addParameter("lastname", "al'vere");

		Predicate predicate = (Predicate) resolver.resolveArgument(getMethodParameterFor("specificFind", Predicate.class),
				null, new ServletWebRequest(request), null);

		assertThat(predicate.toString(), is("user.firstname = EGWENE && user.lastname = al'vere"));
	}

	private MethodParameter getMethodParameterFor(String methodName, Class<?>... args) throws RuntimeException {

		try {
			return new MethodParameter(Sample.class.getMethod(methodName, args), 0);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	static class QDSLSpecic extends QueryDslPredicateSpecification {

		public QDSLSpecic() {
			define("firstname", new QueryDslPredicateBuilder() {

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public Predicate buildPredicate(PropertyPath path, Object value) {

					return new PathBuilderFactory().create(path.getOwningType().getActualType().getType())
							.get(new PathImpl(value.getClass(), PathMetadataFactory.forVariable(path.toDotPath())))
							.eq(value.toString().toUpperCase());
				}
			});
		}
	}

	static interface Sample {

		User predicateWithoutAnnotation(Predicate predicate);

		User nonPredicateWithAnnotation(@QueryDslPredicate String predicate);

		User simpleFind(@QueryDslPredicate Predicate predicate);

		Page<User> pagedFind(@QueryDslPredicate Predicate predicate, Pageable page);

		User specificFind(@QueryDslPredicate(spec = QDSLSpecic.class) Predicate predicate);
	}

}
