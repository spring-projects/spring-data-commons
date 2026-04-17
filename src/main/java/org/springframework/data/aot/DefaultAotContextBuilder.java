/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.aot;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.util.TypeCollector;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link AotContext.AotContextBuilder}.
 *
 * @author Mark Paluch
 * @since 4.0.5
 */
class DefaultAotContextBuilder implements AotContext.AotContextBuilder {

	private @Nullable BeanFactory beanFactory;

	private @Nullable Environment environment;

	private Consumer<TypeCollector> typeCollectorConsumer = it -> {};

	@Override
	public AotContext.AotContextBuilder beanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		return this;
	}

	@Override
	public AotContext.AotContextBuilder environment(Environment environment) {
		this.environment = environment;
		return this;
	}

	@Override
	public AotContext.AotContextBuilder customizeTypeCollector(Consumer<TypeCollector> typeCollectorConsumer) {
		this.typeCollectorConsumer = typeCollectorConsumer;
		return this;
	}

	@Override
	public AotContext build() {

		Assert.notNull(this.beanFactory, "BeanFactory must not be null");

		Environment environment = this.environment;
		if (environment == null) {
			environment = new StandardEnvironment();
		}

		AotMappingContext mappingContext = new AotMappingContext(
				TypeCollector.create(typeCollectorConsumer).getTypeFilter());
		return new DefaultAotContext(beanFactory, environment, mappingContext);
	}

}
