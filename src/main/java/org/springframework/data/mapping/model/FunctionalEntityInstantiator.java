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
import java.util.List;
import java.util.function.Function;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor.Parameter;

/**
 * @author Christoph Strobl
 * @since 2020/10
 */
class FunctionalEntityInstantiator<T> implements EntityInstantiator {

	private final List<String> parameterNames;
	private final Function<Object[], T> newInstanceFunction;

	FunctionalEntityInstantiator(List<String> parameterNames, Function<Object[], T> newInstanceFunction) {

		this.parameterNames = new ArrayList<>(parameterNames);
		this.newInstanceFunction = newInstanceFunction;
	}

	@Override
	public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
			ParameterValueProvider<P> provider) {

		if (parameterNames.isEmpty()) {
			return (T) newInstanceFunction.apply(new Object[] {});
		}

		List<Object> args = new ArrayList<>();
		for (String parameterName : parameterNames) {

			P property = entity.getPersistentProperty(parameterName);

			// TODO: do we need all this information or is the name sufficient
			args.add(provider
					.getParameterValue(new Parameter(parameterName, property.getTypeInformation(), new Annotation[] {}, entity)));
		}

		return (T) newInstanceFunction.apply(args.toArray());
	}
}
