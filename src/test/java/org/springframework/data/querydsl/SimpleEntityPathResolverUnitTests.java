/*
 * Copyright 2011-2015 the original author or authors.
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

import org.junit.Test;

import com.querydsl.core.annotations.QueryEntity;

/**
 * Unit test for {@link SimpleEntityPathResolver}.
 * 
 * @author Oliver Gierke
 */
public class SimpleEntityPathResolverUnitTests {

	EntityPathResolver resolver = SimpleEntityPathResolver.INSTANCE;

	@Test
	public void createsRepositoryFromDomainClassCorrectly() throws Exception {
		assertThat(resolver.createPath(User.class)).isInstanceOf(QUser.class);
	}

	@Test
	public void resolvesEntityPathForInnerClassCorrectly() throws Exception {

		assertThat(resolver.createPath(NamedUser.class)).isInstanceOf(QSimpleEntityPathResolverUnitTests_NamedUser.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsClassWithoutQueryClassConfrmingToTheNamingScheme() throws Exception {

		resolver.createPath(QSimpleEntityPathResolverUnitTests_Sample.class);
	}

	@QueryEntity
	static class Sample {

	}

	@QueryEntity
	static class NamedUser {

	}
}
