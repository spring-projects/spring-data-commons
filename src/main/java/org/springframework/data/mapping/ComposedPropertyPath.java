/*
 * Copyright 2025 the original author or authors.
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

import java.util.Iterator;
import java.util.List;

import org.springframework.data.util.TypeInformation;

record ComposedPropertyPath<T, M, R>(TypedPropertyPath<T, M> first,
		TypedPropertyPath<M, R> second) implements TypedPropertyPath<T, R> {

	@Override
	public R get(T obj) {
		M intermediate = first.get(obj);
		return intermediate != null ? second.get(intermediate) : null;
	}

	@Override
	public TypeInformation<?> getOwningType() {
		return first.getOwningType();
	}

	@Override
	public String getSegment() {
		return first().getSegment();
	}

	@Override
	public PropertyPath getLeafProperty() {
		return second.getLeafProperty();
	}

	@Override
	public TypeInformation<?> getTypeInformation() {
		return first.getTypeInformation();
	}

	@Override
	public PropertyPath next() {
		return second;
	}

	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public String toDotPath() {
		return first.toDotPath() + "." + second.toDotPath();
	}

	@Override
	public Iterator<PropertyPath> iterator() {
		return List.<PropertyPath> of(this, second).iterator();
	}

}
