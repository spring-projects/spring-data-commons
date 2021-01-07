/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Unit tests to reproduce issues reported in DATACMNS-511.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class DataCmns511Tests {

	@Test // DATACMNS-511
	public void detectsEqualTypeVariableTypeInformationInstances() {

		TypeInformation<?> createdBy = ClassTypeInformation.from(AbstractRole.class).getProperty("createdBy");

		assertThat(createdBy.getProperty("roles").getActualType().getProperty("createdBy"))//
				.satisfies(second -> {

					TypeInformation<?> third = second.getProperty("roles").getActualType().getProperty("createdBy");

					assertThat(third).isEqualTo(second);
					assertThat(third.hashCode()).isEqualTo(second.hashCode());
				});
	}

	static class AbstractRole<USER extends AbstractUser<USER, ROLE>, ROLE extends AbstractRole<USER, ROLE>>
			extends AuditingEntity<USER> {

		String name;
	}

	static abstract class AbstractUser<USER extends AbstractUser<USER, ROLE>, ROLE extends AbstractRole<USER, ROLE>> {

		Set<ROLE> roles = new HashSet<>();
	}

	static abstract class AuditingEntity<USER extends AbstractUser<USER, ?>> {

		USER createdBy;
		USER lastModifiedBy;
	}
}
