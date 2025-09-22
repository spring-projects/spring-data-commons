/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.repository.config;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.data.aot.AotContext;
import org.springframework.data.aot.AotTypeConfiguration;
import org.springframework.data.util.TypeScanner;

/**
 * Support class for {@link AotRepositoryContext} implementations delegating to an underlying {@link AotContext}.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public abstract class AotRepositoryContextSupport implements AotRepositoryContext {

	private final AotContext aotContext;

	/**
	 * Create a new {@code AotRepositoryContextSupport} given the {@link AotContext}.
	 *
	 * @param aotContext
	 */
	public AotRepositoryContextSupport(AotContext aotContext) {
		this.aotContext = aotContext;
	}

	@Override
	public boolean isGeneratedRepositoriesEnabled(@Nullable String moduleName) {
		return aotContext.isGeneratedRepositoriesEnabled(moduleName);
	}

	@Override
	public boolean isGeneratedRepositoriesMetadataEnabled() {
		return aotContext.isGeneratedRepositoriesMetadataEnabled();
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() {
		return aotContext.getBeanFactory();
	}

	@Override
	public Environment getEnvironment() {
		return aotContext.getEnvironment();
	}

	@Override
	public @Nullable ClassLoader getClassLoader() {
		return aotContext.getClassLoader();
	}

	@Override
	public ClassLoader getRequiredClassLoader() {
		return aotContext.getRequiredClassLoader();
	}

	@Override
	public TypeScanner getTypeScanner() {
		return aotContext.getTypeScanner();
	}

	@Override
	public void typeConfiguration(ResolvableType resolvableType, Consumer<AotTypeConfiguration> configurationConsumer) {
		aotContext.typeConfiguration(resolvableType, configurationConsumer);
	}

	@Override
	public void typeConfiguration(Class<?> type, Consumer<AotTypeConfiguration> configurationConsumer) {
		aotContext.typeConfiguration(type, configurationConsumer);
	}

	@Override
	public void contributeTypeConfigurations(GenerationContext generationContext) {
		aotContext.contributeTypeConfigurations(generationContext);
	}

}
