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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests to reproduce issues reported in DATACMNS-511.
 * 
 * @author Oliver Gierke
 */
public class DataCmns511Tests {

	/**
	 * @see DATACMNS-511
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void detectsEqualTypeVariableTypeInformationInstances() {

		TypeInformation<AbstractRole> firstRoleType = ClassTypeInformation.from(AbstractRole.class);
		TypeInformation<?> firstCreatedBy = firstRoleType.getProperty("createdBy");
		TypeInformation<?> secondRoleType = firstCreatedBy.getProperty("roles").getActualType();
		TypeInformation secondCreatedBy = secondRoleType.getProperty("createdBy");
		TypeInformation<?> thirdRoleType = secondCreatedBy.getProperty("roles").getActualType();
		TypeInformation thirdCreatedBy = thirdRoleType.getProperty("createdBy");

		assertThat(secondCreatedBy).isEqualTo(thirdCreatedBy);
		assertThat(secondCreatedBy.hashCode()).isEqualTo(thirdCreatedBy.hashCode());
	}

	static class AbstractRole<USER extends AbstractUser<USER, ROLE>, ROLE extends AbstractRole<USER, ROLE>> extends
			AuditingEntity<USER> {

		String name;
	}

	static abstract class AbstractUser<USER extends AbstractUser<USER, ROLE>, ROLE extends AbstractRole<USER, ROLE>> {

		Set<ROLE> roles = new HashSet<ROLE>();
	}

	static abstract class AuditingEntity<USER extends AbstractUser<USER, ?>> {

		USER createdBy;
		USER lastModifiedBy;
	}
}
