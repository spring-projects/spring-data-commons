/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.querydsl;

import static org.hamcrest.collection.IsArrayWithSize.*;
import static org.junit.Assert.*;

import org.hamcrest.collection.IsArrayContainingInOrder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.NullHandling;
import org.springframework.data.keyvalue.Person;
import org.springframework.data.keyvalue.QPerson;

import com.mysema.query.types.EntityPath;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.path.PathBuilder;

/**
 * @author Christoph Strobl
 */
public class QueryDslUtilsUnitTests {

	private EntityPath<Person> path;
	private PathBuilder<Person> builder;

	@Before
	public void setUp() {

		this.path = SimpleEntityPathResolver.INSTANCE.createPath(Person.class);
		this.builder = new PathBuilder<Person>(path.getType(), path.getMetadata());
	}

	@Test(expected = IllegalArgumentException.class)
	public void toOrderSpecifierThrowsExceptioOnNullPathBuilder() {
		QueryDslUtils.toOrderSpecifier(new Sort("firstname"), null);
	}

	@Test
	public void toOrderSpecifierReturnsEmptyArrayWhenSortIsNull() {
		assertThat(QueryDslUtils.toOrderSpecifier(null, builder), arrayWithSize(0));
	}

	@Test
	public void toOrderSpecifierConvertsSimpleAscSortCorrectly() {

		Sort sort = new Sort(Direction.ASC, "firstname");

		OrderSpecifier<?>[] specifiers = QueryDslUtils.toOrderSpecifier(sort, builder);

		assertThat(specifiers, IsArrayContainingInOrder.<OrderSpecifier<?>> arrayContaining(QPerson.person.firstname.asc()));
	}

	@Test
	public void toOrderSpecifierConvertsSimpleDescSortCorrectly() {

		Sort sort = new Sort(Direction.DESC, "firstname");

		OrderSpecifier<?>[] specifiers = QueryDslUtils.toOrderSpecifier(sort, builder);

		assertThat(specifiers,
				IsArrayContainingInOrder.<OrderSpecifier<?>> arrayContaining(QPerson.person.firstname.desc()));
	}

	@Test
	public void toOrderSpecifierConvertsSortCorrectlyAndRetainsArgumentOrder() {

		Sort sort = new Sort(Direction.DESC, "firstname").and(new Sort(Direction.ASC, "age"));

		OrderSpecifier<?>[] specifiers = QueryDslUtils.toOrderSpecifier(sort, builder);

		assertThat(specifiers, IsArrayContainingInOrder.<OrderSpecifier<?>> arrayContaining(
				QPerson.person.firstname.desc(), QPerson.person.age.asc()));
	}

	@Test
	public void toOrderSpecifierConvertsSortWithNullHandlingCorrectly() {

		Sort sort = new Sort(new Sort.Order(Direction.DESC, "firstname", NullHandling.NULLS_LAST));

		OrderSpecifier<?>[] specifiers = QueryDslUtils.toOrderSpecifier(sort, builder);

		assertThat(specifiers,
				IsArrayContainingInOrder.<OrderSpecifier<?>> arrayContaining(QPerson.person.firstname.desc().nullsLast()));
	}

}
