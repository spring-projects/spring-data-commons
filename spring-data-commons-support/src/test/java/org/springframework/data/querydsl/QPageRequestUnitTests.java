/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.querydsl;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AbstractPageRequest;

/**
 * Unit tests for {@link QPageRequest}.
 *
 * @author Thomas Darimont
 * @author Mark Paluch
 */
public class QPageRequestUnitTests extends AbstractPageRequestUnitTests {

	@Override
	public AbstractPageRequest newPageRequest(int page, int size) {
		return QPageRequest.of(page, size);
	}

	@Test
	void constructsQPageRequestWithOrderSpecifiers() {

		var user = QUser.user;
		var pageRequest = QPageRequest.of(0, 10, user.firstname.asc());

		assertThat(pageRequest.getSort()).isEqualTo(QSort.by(user.firstname.asc()));
	}

	@Test
	void constructsQPageRequestWithQSort() {

		var user = QUser.user;
		var pageRequest = QPageRequest.ofSize(10).withSort(QSort.by(user.firstname.asc()));

		assertThat(pageRequest.getSort()).isEqualTo(QSort.by(user.firstname.asc()));
	}

	@Test // DATACMNS-1581
	void rejectsNullSort() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> QPageRequest.of(0, 10, (QSort) null));
	}
}
