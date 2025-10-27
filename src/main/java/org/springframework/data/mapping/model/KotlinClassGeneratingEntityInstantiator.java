/*
 * Copyright 2017-2025 the original author or authors.
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

import java.util.Arrays;

import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.util.KotlinReflectionUtils;

/**
 * Kotlin-specific extension to {@link ClassGeneratingEntityInstantiator} that adapts Kotlin constructors with
 * defaulting.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Johannes Englmeier
 * @since 2.0
 */
class KotlinClassGeneratingEntityInstantiator extends ClassGeneratingEntityInstantiator {

	@Override
	protected EntityInstantiator doCreateEntityInstantiator(PersistentEntity<?, ?> entity) {

		InstanceCreatorMetadata<? extends PersistentProperty<?>> creator = entity.getInstanceCreatorMetadata();

		if (KotlinReflectionUtils.isSupportedKotlinClass(entity.getType())
				&& creator instanceof PreferredConstructor<?, ?> constructor) {

			KotlinInstantiationDelegate delegate = KotlinInstantiationDelegate.resolve(constructor);

			if (delegate != null) {
				ObjectInstantiator instantiator = createObjectInstantiator(entity, delegate.getInstanceCreator());
				return new DefaultingKotlinClassInstantiatorAdapter(instantiator, delegate);
			}
		}

		return super.doCreateEntityInstantiator(entity);
	}

	/**
	 * Entity instantiator for Kotlin constructors that apply parameter defaulting. Kotlin constructors that apply
	 * argument defaulting are marked with {@link kotlin.jvm.internal.DefaultConstructorMarker} and accept additional
	 * parameters besides the regular (user-space) parameters. Additional parameters are:
	 * <ul>
	 * <li>defaulting bitmask ({@code int}), a bit mask slot for each 32 parameters</li>
	 * <li>{@code DefaultConstructorMarker} (usually null)</li>
	 * </ul>
	 * <strong>Defaulting bitmask</strong>
	 * <p>
	 * The defaulting bitmask is a 32 bit integer representing which positional argument should be defaulted. Defaulted
	 * arguments are passed as {@literal null} and require the appropriate positional bit set ( {@code 1 << 2} for the 2.
	 * argument)). Since the bitmask represents only 32 bit states, it requires additional masks (slots) if more than 32
	 * arguments are represented.
	 *
	 * @author Mark Paluch
	 * @since 2.0
	 */
	static class DefaultingKotlinClassInstantiatorAdapter implements EntityInstantiator {

		private final ObjectInstantiator instantiator;
		private final KotlinInstantiationDelegate delegate;

		DefaultingKotlinClassInstantiatorAdapter(ObjectInstantiator instantiator,
				KotlinInstantiationDelegate delegate) {

			this.instantiator = instantiator;
			this.delegate = delegate;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
				ParameterValueProvider<P> provider) {

			Object[] params = allocateArguments(delegate.getRequiredParameterCount());
			delegate.extractInvocationArguments(params, entity.getInstanceCreatorMetadata(), provider);

			try {
				return (T) instantiator.newInstance(params);
			} catch (Exception e) {
				throw new MappingInstantiationException(entity, Arrays.asList(params), e);
			}
		}

	}
}
