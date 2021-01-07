/*
 * Copyright 2008-2021 the original author or authors.
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
package org.springframework.data.repository.query;

import java.lang.reflect.Method;
import java.util.Locale;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Strategy interface for which way to lookup {@link RepositoryQuery}s.
 *
 * @author Oliver Gierke
 */
public interface QueryLookupStrategy {

	public static enum Key {

		CREATE, USE_DECLARED_QUERY, CREATE_IF_NOT_FOUND;

		/**
		 * Returns a strategy key from the given XML value.
		 *
		 * @param xml
		 * @return a strategy key from the given XML value
		 */
		@Nullable
		public static Key create(String xml) {

			if (!StringUtils.hasText(xml)) {
				return null;
			}

			return valueOf(xml.toUpperCase(Locale.US).replace("-", "_"));
		}
	}

	/**
	 * Resolves a {@link RepositoryQuery} from the given {@link QueryMethod} that can be executed afterwards.
	 *
	 * @param method will never be {@literal null}.
	 * @param metadata will never be {@literal null}.
	 * @param factory will never be {@literal null}.
	 * @param namedQueries will never be {@literal null}.
	 * @return
	 */
	RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			NamedQueries namedQueries);
}
