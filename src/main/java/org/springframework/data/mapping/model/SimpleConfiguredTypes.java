/*
 * Copyright 2020. the original author or authors.
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

/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.mapping.model;

import java.lang.annotation.Annotation;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.repository.util.ClassUtils;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 * @since 2020/11
 */
public class SimpleConfiguredTypes {

	private static final ConfigurableTypeInformation<Object> OBJECT_TYPE = new ConfigurableTypeInformation<Object>(Object.class) {

		@Override
		protected void setConstructor(ConfigurableTypeConstructor constructor) {
			throw new IllegalStateException("not allowed");
		}

		@Override
		protected void addAnnotation(Annotation annotation) {
			throw new IllegalStateException("not allowed");
		}

		@Override
		protected void addField(Field field) {
			throw new IllegalStateException("not allowed");
		}

		@Override
		protected void addTypeArguments(TypeInformation[] typeArguments) {
			throw new IllegalStateException("not allowed");
		}
	};

	private static final ConfigurableTypeInformation<String> STRING_TYPE = new ConfigurableTypeInformation<>(String.class);
	private static final ConfigurableTypeInformation<Integer> INT_TYPE = new ConfigurableTypeInformation<>(Integer.class);
	private static final ConfigurableTypeInformation<Long> LONG_TYPE = new ConfigurableTypeInformation<>(Long.class);
	private static final ConfigurableTypeInformation<Date> DATE_TYPE = new ConfigurableTypeInformation<>(Date.class);
	private static final ConfigurableTypeInformation<Float> FLOAT_TYPE = new ConfigurableTypeInformation<>(Float.class);
	private static final ConfigurableTypeInformation<Double> DOUBLE_TYPE = new ConfigurableTypeInformation<>(Double.class);
	private static final ConfigurableTypeInformation<Character> CHAR_TYPE = new ConfigurableTypeInformation<>(Character.class);
	private static final ConfigurableTypeInformation<Byte> BYTE_TYPE = new ConfigurableTypeInformation<>(Byte.class);
	private static final ConfigurableTypeInformation<Short> SHORT_TYPE = new ConfigurableTypeInformation<>(Short.class);
	private static final ConfigurableTypeInformation<Boolean> BOOLEAN_TYPE = new ConfigurableTypeInformation<>(Boolean.class);

	private static final Map<Class<?>, ConfigurableTypeInformation<?>> cached;

	static {

		cached = new LinkedHashMap<>();

		cached.put(OBJECT_TYPE.getType(), OBJECT_TYPE);
		cached.put(STRING_TYPE.getType(), STRING_TYPE);
		cached.put(INT_TYPE.getType(), INT_TYPE);
		cached.put(LONG_TYPE.getType(), LONG_TYPE);
		cached.put(DATE_TYPE.getType(), DATE_TYPE);
		cached.put(FLOAT_TYPE.getType(), FLOAT_TYPE);
		cached.put(DOUBLE_TYPE.getType(), DOUBLE_TYPE);
		cached.put(CHAR_TYPE.getType(), CHAR_TYPE);
		cached.put(BYTE_TYPE.getType(), BYTE_TYPE);
		cached.put(SHORT_TYPE.getType(), SHORT_TYPE);
		cached.put(BOOLEAN_TYPE.getType(), BOOLEAN_TYPE);
	}

	public static boolean isKownSimpleConfiguredType(Class<?> type) {
		return cached.containsKey(org.springframework.util.ClassUtils.resolvePrimitiveIfNecessary(type));
	}

	public static <T> ConfigurableTypeInformation<T> get(Class<T> type) {
		return (ConfigurableTypeInformation<T>) cached.get(org.springframework.util.ClassUtils.resolvePrimitiveIfNecessary(type));
	}

	public static ConfigurableTypeInformation<Object> object() {
		return OBJECT_TYPE;
	}

	public static ConfigurableTypeInformation<String> stringType() {
		return STRING_TYPE;
	}

	public static ConfigurableTypeInformation<Integer> intType() {
		return INT_TYPE;
	}

	public static ConfigurableTypeInformation<Long> longType() {
		return LONG_TYPE;
	}

	public static ConfigurableTypeInformation<Date> dateType() {
		return DATE_TYPE;
	}

	public static ConfigurableTypeInformation<Float> floatType() {
		return FLOAT_TYPE;
	}

	public static ConfigurableTypeInformation<Double> doubleType() {
		return DOUBLE_TYPE;
	}

	public static ConfigurableTypeInformation<Character> charType() {
		return CHAR_TYPE;
	}

	public static ConfigurableTypeInformation<Byte> byteType() {
		return BYTE_TYPE;
	}

	public static ConfigurableTypeInformation<Short> shortType() {
		return SHORT_TYPE;
	}

	public static ConfigurableTypeInformation<Boolean> booleanType() {
		return BOOLEAN_TYPE;
	}
}
