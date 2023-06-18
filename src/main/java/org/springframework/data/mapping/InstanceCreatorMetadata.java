/*
 * Copyright 2021-2023 the original author or authors.
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

import java.util.List;

/**
 * Metadata describing a mechanism to create instances of persistent types.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 * @since 3.0
 */
public interface InstanceCreatorMetadata<P extends PersistentProperty<P>> {

	/**
	 * Check whether the given {@link PersistentProperty} is being used as creator parameter.
	 *
	 * @param property
	 * @return
	 */
	boolean isCreatorParameter(PersistentProperty<?> property);

	/**
	 * Returns whether the given {@link Parameter} is one referring to parent value (such as an enclosing class or a
	 * receiver parameter).
	 *
	 * @param parameter
	 * @return
	 */
	default boolean isParentParameter(Parameter<?, P> parameter) {
		return false;
	}

	/**
	 * @return the number of parameters.
	 */
	default int getParameterCount() {
		return getParameters().size();
	}

	/**
	 * @return the parameters used by this creator.
	 */
	List<Parameter<Object, P>> getParameters();

	/**
	 * @return whether the creator accepts {@link Parameter}s.
	 */
	default boolean hasParameters() {
		return !getParameters().isEmpty();
	}
}
