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

import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class to set and retrieve bean values.
 * 
 * @author Jon Brisbin <jbrisbin@vmware.com>
 * @author Oliver Gierke
 */
public abstract class MappingBeanHelper {

	private static final Set<Class<?>> simpleTypes = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

	static {
		simpleTypes.add(boolean.class);
		simpleTypes.add(boolean[].class);
		simpleTypes.add(long.class);
		simpleTypes.add(long[].class);
		simpleTypes.add(short.class);
		simpleTypes.add(short[].class);
		simpleTypes.add(int.class);
		simpleTypes.add(int[].class);
		simpleTypes.add(byte.class);
		simpleTypes.add(byte[].class);
		simpleTypes.add(float.class);
		simpleTypes.add(float[].class);
		simpleTypes.add(double.class);
		simpleTypes.add(double[].class);
		simpleTypes.add(char.class);
		simpleTypes.add(char[].class);
		simpleTypes.add(Boolean.class);
		simpleTypes.add(Long.class);
		simpleTypes.add(Short.class);
		simpleTypes.add(Integer.class);
		simpleTypes.add(Byte.class);
		simpleTypes.add(Float.class);
		simpleTypes.add(Double.class);
		simpleTypes.add(Character.class);
		simpleTypes.add(String.class);
		simpleTypes.add(Date.class);
		simpleTypes.add(Locale.class);
		simpleTypes.add(Class.class);
	}

	/**
	 * Returns the set of types considered to be simple.
	 * 
	 * @return
	 */
	public static Set<Class<?>> getSimpleTypes() {
		return simpleTypes;
	}

	/**
	 * Returns whether the given type is considered a simple one.
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isSimpleType(Class<?> type) {
		for (Class<?> clazz : simpleTypes) {
			if (type == clazz || type.isAssignableFrom(clazz)) {
				return true;
			}
		}
		return type.isEnum();
	}
}
