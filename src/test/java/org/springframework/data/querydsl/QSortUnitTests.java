/*
 * Copyright 2013 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import com.mysema.query.types.OrderSpecifier;

/**
 * Unit tests for {@link QSort}.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
public class QSortUnitTests {

	/**
	 * @see DATACMNS-402
	 */
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIfNoOrderSpecifiersAreGiven() {
		new QSort();
	}

	/**
	 * @see DATACMNS-402
	 */
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIfNullIsGiven() {
		new QSort((List<OrderSpecifier<?>>) null);
	}

	/**
	 * @see DATACMNS-402
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void sortBySingleProperty() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc());

		assertThat(qsort.getOrderSpecifiers().size(), is(1));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(0), is(user.firstname.asc()));
		assertThat(qsort.getOrderFor("firstname"), is(new Sort.Order(Sort.Direction.ASC, "firstname")));
	}

	/**
	 * @see DATACMNS-402
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void sortByMultiplyProperties() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc(), user.lastname.desc());

		assertThat(qsort.getOrderSpecifiers().size(), is(2));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(0), is(user.firstname.asc()));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(1), is(user.lastname.desc()));
		assertThat(qsort.getOrderFor("firstname"), is(new Sort.Order(Sort.Direction.ASC, "firstname")));
		assertThat(qsort.getOrderFor("lastname"), is(new Sort.Order(Sort.Direction.DESC, "lastname")));
	}

	/**
	 * @see DATACMNS-402
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void sortByMultiplyPropertiesWithAnd() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc()).and(new QSort(user.lastname.desc()));

		assertThat(qsort.getOrderSpecifiers().size(), is(2));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(0), is(user.firstname.asc()));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(1), is(user.lastname.desc()));
		assertThat(qsort.getOrderFor("firstname"), is(new Sort.Order(Sort.Direction.ASC, "firstname")));
		assertThat(qsort.getOrderFor("lastname"), is(new Sort.Order(Sort.Direction.DESC, "lastname")));
	}

	/**
	 * @see DATACMNS-402
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void sortByMultiplyPropertiesWithAndAndVarArgs() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc()).and(user.lastname.desc());

		assertThat(qsort.getOrderSpecifiers().size(), is(2));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(0), is(user.firstname.asc()));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(1), is(user.lastname.desc()));
		assertThat(qsort.getOrderFor("firstname"), is(new Sort.Order(Sort.Direction.ASC, "firstname")));
		assertThat(qsort.getOrderFor("lastname"), is(new Sort.Order(Sort.Direction.DESC, "lastname")));
	}

	/**
	 * @see DATACMNS-402
	 */
	@Test
	public void ensureInteroperabilityWithSort() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc(), user.lastname.desc());

		Sort sort = qsort;

		assertThat(sort.getOrderFor("firstname"), is(new Sort.Order(Sort.Direction.ASC, "firstname")));
		assertThat(sort.getOrderFor("lastname"), is(new Sort.Order(Sort.Direction.DESC, "lastname")));
	}

	/**
	 * @see DATACMNS-402
	 */
	@Test
	public void concatenatesPlainSortCorrectly() {

		QUser user = QUser.user;
		QSort sort = new QSort(user.firstname.asc());

		Sort result = sort.and(new Sort(Direction.ASC, "lastname"));
		assertThat(result, is(Matchers.<Order> iterableWithSize(2)));
		assertThat(result, hasItems(new Order(Direction.ASC, "lastname"), new Order(Direction.ASC, "firstname")));
	}
}
