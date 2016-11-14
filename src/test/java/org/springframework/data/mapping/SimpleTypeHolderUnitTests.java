/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Unit tests for {@link SimpleTypeHolder}.
 * 
 * @author Oliver Gierke
 */
public class SimpleTypeHolderUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullCustomTypes() {
		new SimpleTypeHolder(null, false);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullOriginal() {
		new SimpleTypeHolder(new HashSet<Class<?>>(), null);
	}

	/**
	 * @see DATACMNS-31
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTypeForIsSimpleTypeCall() {
		SimpleTypeHolder holder = new SimpleTypeHolder();
		holder.isSimpleType(null);
	}

	@Test
	public void addsDefaultTypes() {

		SimpleTypeHolder holder = new SimpleTypeHolder();

		assertThat(holder.isSimpleType(String.class)).isTrue();
	}

	@Test
	public void doesNotAddDefaultConvertersIfConfigured() {

		SimpleTypeHolder holder = new SimpleTypeHolder(new HashSet<Class<?>>(), false);

		assertThat(holder.isSimpleType(String.class)).isFalse();
	}

	@Test
	public void addsCustomTypesToSimpleOnes() {

		SimpleTypeHolder holder = new SimpleTypeHolder(Collections.singleton(SimpleTypeHolder.class), true);

		assertThat(holder.isSimpleType(SimpleTypeHolder.class)).isTrue();
		assertThat(holder.isSimpleType(SimpleTypeHolderUnitTests.class)).isFalse();
	}

	@Test
	public void createsHolderFromAnotherOneCorrectly() {

		SimpleTypeHolder holder = new SimpleTypeHolder(Collections.singleton(SimpleTypeHolder.class), true);
		SimpleTypeHolder second = new SimpleTypeHolder(Collections.singleton(SimpleTypeHolderUnitTests.class), holder);

		assertThat(holder.isSimpleType(SimpleTypeHolder.class)).isTrue();
		assertThat(holder.isSimpleType(SimpleTypeHolderUnitTests.class)).isFalse();
		assertThat(second.isSimpleType(SimpleTypeHolder.class)).isTrue();
		assertThat(second.isSimpleType(SimpleTypeHolderUnitTests.class)).isTrue();
	}

	@Test
	public void considersObjectToBeSimpleType() {
		SimpleTypeHolder holder = new SimpleTypeHolder();
		assertThat(holder.isSimpleType(Object.class)).isTrue();
	}

	@Test
	public void considersSimpleEnumAsSimple() {

		SimpleTypeHolder holder = new SimpleTypeHolder();
		assertThat(holder.isSimpleType(SimpleEnum.FOO.getClass())).isTrue();
	}

	@Test
	public void considersComplexEnumAsSimple() {

		SimpleTypeHolder holder = new SimpleTypeHolder();
		assertThat(holder.isSimpleType(ComplexEnum.FOO.getClass())).isTrue();
	}

	enum SimpleEnum {

		FOO;
	}

	enum ComplexEnum {

		FOO {
			@Override
			boolean method() {
				return false;
			}
		};

		abstract boolean method();
	}
}
