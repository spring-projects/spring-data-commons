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

import javax.annotation.Nullable;

import org.springframework.data.mapping.Alias;
import org.springframework.data.util.TypeInformation;

/**
 * Interface to abstract the mapping from a type alias to the actual type.
 *
 * @author Oliver Gierke
 */
public interface TypeInformationMapper {

	/**
	 * Returns the actual {@link TypeInformation} to be used for the given alias.
	 *
	 * @param alias must not be {@literal null}.
	 * @return
	 */
	@Nullable
	TypeInformation<?> resolveTypeFrom(Alias alias);

	/**
	 * Returns the alias to be used for the given {@link TypeInformation}.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	Alias createAliasFor(TypeInformation<?> type);
}
