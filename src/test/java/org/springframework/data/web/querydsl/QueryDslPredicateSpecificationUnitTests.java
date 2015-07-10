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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.data.querydsl.Address;
import org.springframework.data.querydsl.QAddress;
import org.springframework.data.util.ClassTypeInformation;

import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.StringPath;

/**
 * Unit tests for {@link QueryDslPredicateSpecification}.
 * 
 * @author Oliver Gierke
 */
public class QueryDslPredicateSpecificationUnitTests {

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void usesTypeBasedBindingIfConfigured() {

		QueryDslPredicateSpecification specification = new QueryDslPredicateSpecification() {
			{
				define(String.class, new QueryDslPredicateBuilder<StringPath>() {

					@Override
					public Predicate buildPredicate(StringPath path, Object value) {
						return path.contains(value.toString());
					}
				});
			}
		};

		MutablePropertyValues values = new MutablePropertyValues(Collections.singletonMap("city", "Dresden"));
		QueryDslPredicateAccessor accessor = new QueryDslPredicateAccessor(ClassTypeInformation.from(Address.class),
				specification);

		assertThat(accessor.getPredicate(values), is((Predicate) QAddress.address.city.contains("Dresden")));
	}
}
