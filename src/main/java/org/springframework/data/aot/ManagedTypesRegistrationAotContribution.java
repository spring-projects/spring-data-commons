/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.aot;

import java.util.List;
import java.util.function.BiConsumer;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.core.ResolvableType;
import org.springframework.data.ManagedTypes;
import org.springframework.lang.NonNull;

/**
 * {@link BeanRegistrationAotContribution} used to contribute a {@link ManagedTypes} registration.
 *
 * @author John Blum
 * @see org.springframework.beans.factory.aot.BeanRegistrationAotContribution
 * @since 3.0.0
 */
public class ManagedTypesRegistrationAotContribution implements BeanRegistrationAotContribution {

	private final AotContext aotContext;
	private final ManagedTypes managedTypes;
	private final BiConsumer<ResolvableType, GenerationContext> contributionAction;

	public ManagedTypesRegistrationAotContribution(AotContext aotContext, ManagedTypes managedTypes,
			@NonNull BiConsumer<ResolvableType, GenerationContext> contributionAction) {

		this.aotContext = aotContext;
		this.managedTypes = managedTypes;
		this.contributionAction = contributionAction;
	}

	protected AotContext getAotContext() {
		return this.aotContext;
	}

	@NonNull
	protected ManagedTypes getManagedTypes() {
		return ManagedTypes.nullSafeManagedTypes(this.managedTypes);
	}

	@Override
	public void applyTo(@NonNull GenerationContext generationContext,
			@NonNull BeanRegistrationCode beanRegistrationCode) {

		List<Class<?>> types = getManagedTypes().toList();

		if (!types.isEmpty()) {
			TypeCollector.inspect(types).forEach(type -> contributionAction.accept(type, generationContext));
		}
	}
}
