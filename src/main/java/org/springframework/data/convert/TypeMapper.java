/*
 * Copyright 2011-2018 the original author or authors.
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
package org.springframework.data.convert;

import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * Interface to define strategies how to store type information in a store specific sink or source.
 *
 * @author Oliver Gierke
 */
public interface TypeMapper<S> {

	/**
	 * Reads the {@link TypeInformation} from the given source.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	@Nullable
	TypeInformation<?> readType(S source);

	/**
	 * Returns the {@link TypeInformation} from the given source if it is a more concrete type than the given default one.
	 *
	 * @param source must not be {@literal null}.
	 * @param defaultType must not be {@literal null}.
	 * @return
	 */
	<T> TypeInformation<? extends T> readType(S source, TypeInformation<T> defaultType);

	/**
	 * Writes type information for the given type into the given sink.
	 *
	 * @param type must not be {@literal null}.
	 * @param dbObject must not be {@literal null}.
	 */
	void writeType(Class<?> type, S dbObject);

	/**
	 * Writes type information for the given {@link TypeInformation} into the given sink.
	 *
	 * @param type must not be {@literal null}.
	 * @param dbObject must not be {@literal null}.
	 */
	void writeType(TypeInformation<?> type, S dbObject);
}
