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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.mysema.query.types.OrderSpecifier;

/**
 * @author Thomas Darimont
 */
public class QSortUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIfNoOrderSpecifiersAreGiven() {
		new QSort();
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIfNullIsGiven() {
		new QSort((List<OrderSpecifier<?>>) null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void sortBySingleProperty() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc());

		assertThat(qsort.getOrderSpecifiers().size(), is(1));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(0), is(user.firstname.asc()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void sortByMultiplyProperties() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc(), user.lastname.desc());

		assertThat(qsort.getOrderSpecifiers().size(), is(2));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(0), is(user.firstname.asc()));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(1), is(user.lastname.desc()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void sortByMultiplyPropertiesWithAnd() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc()).and(new QSort(user.lastname.desc()));

		assertThat(qsort.getOrderSpecifiers().size(), is(2));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(0), is(user.firstname.asc()));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(1), is(user.lastname.desc()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void sortByMultiplyPropertiesWithAndAndVarArgs() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc()).and(user.lastname.desc());

		assertThat(qsort.getOrderSpecifiers().size(), is(2));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(0), is(user.firstname.asc()));
		assertThat((OrderSpecifier<String>) qsort.getOrderSpecifiers().get(1), is(user.lastname.desc()));
	}
}
