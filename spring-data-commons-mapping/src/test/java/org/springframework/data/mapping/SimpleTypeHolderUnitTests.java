/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.mapping;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Unit tests for {@link SimpleTypeHolder}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class SimpleTypeHolderUnitTests {

	@Test
	public void rejectsNullCustomTypes() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleTypeHolder(null, false));
	}

	@Test
	public void rejectsNullOriginal() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleTypeHolder(new HashSet<>(), null));
	}

	@Test // DATACMNS-31
	public void rejectsNullTypeForIsSimpleTypeCall() {

		var holder = SimpleTypeHolder.DEFAULT;

		assertThatIllegalArgumentException().isThrownBy(() -> holder.isSimpleType(null));
	}

	@Test
	public void addsDefaultTypes() {

		var holder = SimpleTypeHolder.DEFAULT;

		assertThat(holder.isSimpleType(String.class)).isTrue();
	}

	@Test
	public void doesNotAddDefaultConvertersIfConfigured() {

		var holder = new SimpleTypeHolder(new HashSet<>(), false);

		assertThat(holder.isSimpleType(UUID.class)).isFalse();
	}

	@Test
	public void addsCustomTypesToSimpleOnes() {

		var holder = new SimpleTypeHolder(Collections.singleton(SimpleTypeHolder.class), true);

		assertThat(holder.isSimpleType(SimpleTypeHolder.class)).isTrue();
		assertThat(holder.isSimpleType(SimpleTypeHolderUnitTests.class)).isFalse();
	}

	@Test
	public void createsHolderFromAnotherOneCorrectly() {

		var holder = new SimpleTypeHolder(Collections.singleton(SimpleTypeHolder.class), true);
		var second = new SimpleTypeHolder(Collections.singleton(SimpleTypeHolderUnitTests.class), holder);

		assertThat(holder.isSimpleType(SimpleTypeHolder.class)).isTrue();
		assertThat(holder.isSimpleType(SimpleTypeHolderUnitTests.class)).isFalse();
		assertThat(second.isSimpleType(SimpleTypeHolder.class)).isTrue();
		assertThat(second.isSimpleType(SimpleTypeHolderUnitTests.class)).isTrue();
	}

	@Test
	public void considersObjectToBeSimpleType() {
		var holder = SimpleTypeHolder.DEFAULT;
		assertThat(holder.isSimpleType(Object.class)).isTrue();
	}

	@Test
	public void considersSimpleEnumAsSimple() {

		var holder = SimpleTypeHolder.DEFAULT;
		assertThat(holder.isSimpleType(SimpleEnum.FOO.getClass())).isTrue();
	}

	@Test
	public void considersComplexEnumAsSimple() {

		var holder = SimpleTypeHolder.DEFAULT;
		assertThat(holder.isSimpleType(ComplexEnum.FOO.getClass())).isTrue();
	}

	@Test // DATACMNS-1006
	public void considersJavaLangTypesSimple() {

		var holder = SimpleTypeHolder.DEFAULT;

		assertThat(holder.isSimpleType(Type.class)).isTrue();
	}

	@Test // DATACMNS-1294
	public void considersJavaTimeTypesSimple() {

		var holder = SimpleTypeHolder.DEFAULT;

		assertThat(holder.isSimpleType(Instant.class)).isTrue();
	}

	@Test // DATACMNS-1101
	public void considersExtendedTypeAsSimple() {

		var holder = SimpleTypeHolder.DEFAULT;

		assertThat(holder.isSimpleType(ExtendedPerson.class)).isFalse();
	}

	@Test // DATACMNS-1101
	public void considersExtendedTypeAsSimpleSeenBaseClassBefore() {

		var holder = SimpleTypeHolder.DEFAULT;

		assertThat(holder.isSimpleType(Person.class)).isFalse();
		assertThat(holder.isSimpleType(ExtendedPerson.class)).isFalse();
	}

	@Test // DATACMNS-1278
	public void alwaysConsidersEnumsSimple() {

		var holder = SimpleTypeHolder.DEFAULT;

		assertThat(holder.isSimpleType(SomeInterface.class)).isFalse();
		assertThat(holder.isSimpleType(InterfacedEnum.class)).isTrue();
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

	static class Person {

	}

	static class ExtendedPerson extends Person {

	}

	interface SomeInterface {}

	enum InterfacedEnum implements SomeInterface {}
}
