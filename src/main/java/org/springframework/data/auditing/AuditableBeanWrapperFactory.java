/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.auditing;

import java.util.Optional;

/**
 * A factory to lookup {@link AuditableBeanWrapper}s.
 * 
 * @author Oliver Gierke
 * @since 1.10
 */
public interface AuditableBeanWrapperFactory {

	/**
	 * Returns the {@link AuditableBeanWrapper} for the given source object if it's eligible for auditing.
	 * 
	 * @param source.
	 * @return the {@link AuditableBeanWrapper} for the given source object if it's eligible for auditing.
	 */
	Optional<AuditableBeanWrapper> getBeanWrapperFor(Optional<? extends Object> source);
}
