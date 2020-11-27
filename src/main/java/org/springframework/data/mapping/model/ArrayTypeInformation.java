/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.mapping.model;

import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 * @since 2020/11
 */
public class ArrayTypeInformation<T> extends ConfigurableTypeInformation<T> {

	public ArrayTypeInformation(TypeInformation<T> type) {
		super(type.getType(), type, null);
	}

	public static ArrayTypeInformation array() {
		return new ArrayTypeInformation<>(SimpleConfiguredTypes.object());
	}

	public static <S> ArrayTypeInformation<S[]> arrayOf(TypeInformation<?> componentType) {
		return new ArrayTypeInformation(componentType);
	}

	@Override
	public boolean isCollectionLike() {
		return true;
	}
}
