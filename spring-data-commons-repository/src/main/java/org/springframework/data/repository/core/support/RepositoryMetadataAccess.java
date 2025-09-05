/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.data.repository.core.support;

/**
 * Marker for repository fragment implementation that intend to access repository method invocation metadata.
 * <p>
 * Note that this is a marker interface in the style of {@link java.io.Serializable}, semantically applying to a
 * fragment implementation class. In other words, this marker applies to a particular repository composition that
 * enables metadata access for the repository proxy when the composition contain fragments implementing this interface.
 * <p>
 * Ideally, in a repository composition only the fragment implementation uses this interface while the fragment
 * interface does not.
 *
 * @author Mark Paluch
 * @since 3.4
 * @see org.springframework.data.repository.core.RepositoryMethodContext
 */
public interface RepositoryMetadataAccess {

}
