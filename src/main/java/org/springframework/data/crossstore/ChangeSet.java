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
package org.springframework.data.crossstore;

import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.core.convert.ConversionService;

/**
 * Interface representing the set of changes in an entity.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 */
public interface ChangeSet {

	@Nullable
	<T> T get(String key, Class<T> requiredClass, ConversionService cs);

	void set(String key, Object o);

	Map<String, Object> getValues();

	@Nullable
	Object removeProperty(String k);

}
