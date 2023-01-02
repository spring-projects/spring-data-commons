/*
 * Copyright 2022-2023 the original author or authors.
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

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * Value object to encapsulate the entity creation mechanism through a {@link Executable} to be used when mapping
 * persistent data to objects.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 * @since 3.0
 */
class InstanceCreatorMetadataSupport<T, P extends PersistentProperty<P>> implements InstanceCreatorMetadata<P> {

	private final Executable executable;
	private final List<Parameter<Object, P>> parameters;
	private final Map<PersistentProperty<?>, Boolean> isPropertyParameterCache = new ConcurrentHashMap<>();

	/**
	 * Creates a new {@link InstanceCreatorMetadataSupport} from the given {@link Executable} and {@link Parameter}s.
	 *
	 * @param executable must not be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 */
	@SafeVarargs
	public InstanceCreatorMetadataSupport(Executable executable, Parameter<Object, P>... parameters) {

		Assert.notNull(executable, "Executable must not be null");
		Assert.notNull(parameters, "Parameters must not be null");

		this.executable = executable;
		this.parameters = Arrays.asList(parameters);
	}

	/**
	 * Returns the underlying {@link Executable} that can be invoked reflectively.
	 *
	 * @return
	 */
	Executable getExecutable() {
		return executable;
	}

	/**
	 * Returns the {@link Parameter}s of the executable.
	 *
	 * @return
	 */
	public List<Parameter<Object, P>> getParameters() {
		return parameters;
	}

	/**
	 * Returns whether the given {@link PersistentProperty} is referenced in a creator argument of the
	 * {@link PersistentEntity} backing this {@link InstanceCreatorMetadataSupport}.
	 * <p>
	 * Results of this call are cached and reused on the next invocation. Calling this method for a
	 * {@link PersistentProperty} that was not yet added to its owning {@link PersistentEntity} will capture that state
	 * and return the same result after adding {@link PersistentProperty} to its entity.
	 *
	 * @param property must not be {@literal null}.
	 * @return {@literal true} if the {@link PersistentProperty} is used in the creator.
	 */
	@Override
	public boolean isCreatorParameter(PersistentProperty<?> property) {

		Assert.notNull(property, "Property must not be null");

		Boolean cached = isPropertyParameterCache.get(property);

		if (cached != null) {
			return cached;
		}

		boolean result = doGetIsCreatorParameter(property);

		isPropertyParameterCache.put(property, result);

		return result;
	}

	@Override
	public String toString() {
		return executable.toString();
	}

	private boolean doGetIsCreatorParameter(PersistentProperty<?> property) {

		for (Parameter<?, P> parameter : parameters) {
			if (parameter.maps(property)) {
				return true;
			}
		}

		return false;
	}
}
