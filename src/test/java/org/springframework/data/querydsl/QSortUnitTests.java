/*
 * Copyright 2013-2015 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.querydsl.QQSortUnitTests_WrapperToWrapWrapperForUserWrapper.*;
import static org.springframework.data.querydsl.QQSortUnitTests_WrapperToWrapWrapperForUserWrapper_WrapperForUserWrapper.*;
import static org.springframework.data.querydsl.QQSortUnitTests_WrapperToWrapWrapperForUserWrapper_WrapperForUserWrapper_UserWrapper.*;

import java.util.List;

import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import com.querydsl.core.annotations.QueryInit;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilderFactory;
import com.querydsl.core.types.dsl.StringPath;

/**
 * Unit tests for {@link QSort}.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class QSortUnitTests {

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
	@Test
	public void sortBySingleProperty() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc());

		assertThat(qsort.getOrderSpecifiers()).hasSize(1);
		assertThat(qsort.getOrderSpecifiers().get(0)).isEqualTo(user.firstname.asc());
		assertThat(qsort.getOrderFor("firstname")).isEqualTo(new Sort.Order(Sort.Direction.ASC, "firstname"));
	}

	/**
	 * @see DATACMNS-402
	 */
	@Test
	public void sortByMultiplyProperties() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc(), user.lastname.desc());

		assertThat(qsort.getOrderSpecifiers()).hasSize(2);
		assertThat(qsort.getOrderSpecifiers().get(0)).isEqualTo(user.firstname.asc());
		assertThat(qsort.getOrderSpecifiers().get(1)).isEqualTo(user.lastname.desc());
		assertThat(qsort.getOrderFor("firstname")).isEqualTo(new Sort.Order(Sort.Direction.ASC, "firstname"));
		assertThat(qsort.getOrderFor("lastname")).isEqualTo(new Sort.Order(Sort.Direction.DESC, "lastname"));
	}

	/**
	 * @see DATACMNS-402
	 */
	@Test
	public void sortByMultiplyPropertiesWithAnd() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc()).and(new QSort(user.lastname.desc()));

		assertThat(qsort.getOrderSpecifiers()).hasSize(2);
		assertThat(qsort.getOrderSpecifiers().get(0)).isEqualTo(user.firstname.asc());
		assertThat(qsort.getOrderSpecifiers().get(1)).isEqualTo(user.lastname.desc());
		assertThat(qsort.getOrderFor("firstname")).isEqualTo(new Sort.Order(Sort.Direction.ASC, "firstname"));
		assertThat(qsort.getOrderFor("lastname")).isEqualTo(new Sort.Order(Sort.Direction.DESC, "lastname"));
	}

	/**
	 * @see DATACMNS-402
	 */
	@Test
	public void sortByMultiplyPropertiesWithAndAndVarArgs() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc()).and(user.lastname.desc());

		assertThat(qsort.getOrderSpecifiers()).hasSize(2);
		assertThat(qsort.getOrderSpecifiers().get(0)).isEqualTo(user.firstname.asc());
		assertThat(qsort.getOrderSpecifiers().get(1)).isEqualTo(user.lastname.desc());
		assertThat(qsort.getOrderFor("firstname")).isEqualTo(new Sort.Order(Sort.Direction.ASC, "firstname"));
		assertThat(qsort.getOrderFor("lastname")).isEqualTo(new Sort.Order(Sort.Direction.DESC, "lastname"));
	}

	/**
	 * @see DATACMNS-402
	 */
	@Test
	public void ensureInteroperabilityWithSort() {

		QUser user = QUser.user;
		QSort qsort = new QSort(user.firstname.asc(), user.lastname.desc());

		Sort sort = qsort;

		assertThat(sort.getOrderFor("firstname")).isEqualTo(new Sort.Order(Sort.Direction.ASC, "firstname"));
		assertThat(sort.getOrderFor("lastname")).isEqualTo(new Sort.Order(Sort.Direction.DESC, "lastname"));
	}

	/**
	 * @see DATACMNS-402
	 */
	@Test
	public void concatenatesPlainSortCorrectly() {

		QUser user = QUser.user;
		QSort sort = new QSort(user.firstname.asc());

		Sort result = sort.and(new Sort(Direction.ASC, "lastname"));
		assertThat(result).hasSize(2);
		assertThat(result).contains(new Order(Direction.ASC, "lastname"), new Order(Direction.ASC, "firstname"));
	}

	/**
	 * @see DATACMNS-566
	 */
	@Test
	public void shouldSupportSortByOperatorExpressions() {

		QUser user = QUser.user;
		QSort sort = new QSort(user.dateOfBirth.yearMonth().asc());

		Sort result = sort.and(new Sort(Direction.ASC, "lastname"));
		assertThat(result).hasSize(2);
		assertThat(result).contains(new Order(Direction.ASC, "lastname"),
				new Order(Direction.ASC, user.dateOfBirth.yearMonth().toString()));
	}

	/**
	 * @see DATACMNS-621
	 */
	@Test
	public void shouldCreateSortForNestedPathCorrectly() {

		QSort sort = new QSort(userWrapper.user.firstname.asc());

		assertThat(sort).contains(new Order(Direction.ASC, "user.firstname"));
	}

	/**
	 * @see DATACMNS-621
	 */
	@Test
	public void shouldCreateSortForDeepNestedPathCorrectly() {

		QSort sort = new QSort(wrapperForUserWrapper.wrapper.user.firstname.asc());

		assertThat(sort).contains(new Order(Direction.ASC, "wrapper.user.firstname"));
	}

	/**
	 * @see DATACMNS-621
	 */
	@Test
	public void shouldCreateSortForReallyDeepNestedPathCorrectly() {

		QSort sort = new QSort(wrapperToWrapWrapperForUserWrapper.wrapperForUserWrapper.wrapper.user.firstname.asc());

		assertThat(sort).contains(new Order(Direction.ASC, "wrapperForUserWrapper.wrapper.user.firstname"));
	}

	/**
	 * @see DATACMNS-755
	 */
	@Test
	public void handlesPlainStringPathsCorrectly() {

		StringPath path = new PathBuilderFactory().create(User.class).getString("firstname");

		QSort sort = new QSort(new OrderSpecifier<>(com.querydsl.core.types.Order.ASC, path));

		assertThat(sort).contains(new Order(Direction.ASC, "firstname"));
	}

	@com.querydsl.core.annotations.QueryEntity
	static class WrapperToWrapWrapperForUserWrapper {

		@QueryInit("wrapper.user") //
		WrapperForUserWrapper wrapperForUserWrapper;

		@com.querydsl.core.annotations.QueryEntity
		static class WrapperForUserWrapper {

			UserWrapper wrapper;

			@com.querydsl.core.annotations.QueryEntity
			static class UserWrapper {

				User user;
			}
		}
	}
}
