/*
 * Copyright 2020 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2020/10
 */
public class DomainTypeConstructor<T> extends PreferredConstructor implements EntityInstantiatorAware {

	private List<String> args;

	public DomainTypeConstructor(List<String> args) {
		this.args = args;
	}

	public static DomainTypeConstructor of(String... args) {
		return new DomainTypeConstructor(Arrays.asList(args));
	}

	public static <T> DomainTypeConstructor<T> of(Function<Object[], T> newInstanceFunction, String... parameterNames) {

		return new DomainTypeConstructor<T>(Arrays.asList(parameterNames)) {

			@Nullable
			@Override
			public EntityInstantiator getEntityInstantiator() {

				return new EntityInstantiator() {
					@Override
					public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(
							E entity, ParameterValueProvider<P> provider) {

						List<Object> args = new ArrayList<>();
						for (String parameterName : parameterNames) {

							P property = entity.getPersistentProperty(parameterName);

							// TODO: do we need all this information or is the name sufficient
							args.add(provider.getParameterValue(
									new Parameter(parameterName, property.getTypeInformation(), new Annotation[] {}, entity)));
						}

						return (T) newInstanceFunction.apply(args.toArray());
					}
				};
			}
		};
	}

	@Override
	public boolean isConstructorParameter(PersistentProperty property) {

		if (args.contains(property.getName())) {
			return true;
		}

		return super.isConstructorParameter(property);
	}

	@Override
	public boolean hasParameters() {
		return !args.isEmpty();
	}

	@Nullable
	@Override
	public EntityInstantiator getEntityInstantiator() {
		return null;
	}
}
