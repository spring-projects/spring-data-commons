/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.repository.query.spi;

import java.util.Collections;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * A base class for {@link EvaluationContextExtension}s.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @see 1.9
 */
public abstract class EvaluationContextExtensionSupport implements EvaluationContextExtension {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.EvaluationContextExtension#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		return Collections.emptyMap();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.EvaluationContextExtension#getFunctions()
	 */
	@Override
	public Map<String, Function> getFunctions() {
		return Collections.emptyMap();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.spi.EvaluationContextExtension#getRootObject()
	 */
	@Nullable
	@Override
	public Object getRootObject() {
		return null;
	}
}
