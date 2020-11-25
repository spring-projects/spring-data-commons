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

import java.util.List;

import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 * @since 2020/10
 */
public class ListTypeInformation<S> extends ConfigurableTypeInformation<List<S>> {

	public ListTypeInformation(TypeInformation<S> componentType) {
		super((Class) List.class, componentType, null);
	}

	public static ListTypeInformation list() {
		return new ListTypeInformation<>(SimpleConfiguredTypes.object());
	}

	public static <S> ListTypeInformation<S> listOf(TypeInformation<S> componentType) {
		return new ListTypeInformation<>(componentType);
	}

	@Override
	public boolean isCollectionLike() {
		return true;
	}
}
