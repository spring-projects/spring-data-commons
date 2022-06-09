/*
 * Copyright 2022 the original author or authors.
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

import java.util.Arrays;
import java.util.Properties;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.io.InputStreamSource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.core.support.RepositoryFragmentsFactoryBean;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 3.0
 */
public class DataRuntimeHints implements RuntimeHintsRegistrar {

	/*
	NativeHint(trigger = MappingContext.class,
		types = {
				@TypeHint(types = {
						RepositoryFactoryBeanSupport.class,
						RepositoryFragmentsFactoryBean.class,
						RepositoryFragment.class,
						TransactionalRepositoryFactoryBeanSupport.class,
						QueryByExampleExecutor.class,
						MappingContext.class,
						RepositoryMetadata.class,
						RepositoryMetadata.class,
				}),
				@TypeHint(types = {ReadingConverter.class, WritingConverter.class}),
				@TypeHint(types = {Properties.class, BeanFactory.class, InputStreamSource[].class}),
				@TypeHint(types = Throwable.class, access = { TypeAccess.DECLARED_CONSTRUCTORS, TypeAccess.DECLARED_FIELDS}),
				@TypeHint(typeNames = {
						"org.springframework.data.projection.SpelEvaluatingMethodInterceptor$TargetWrapper",
				}, access = { TypeAccess.DECLARED_CONSTRUCTORS, TypeAccess.DECLARED_METHODS, TypeAccess.PUBLIC_METHODS })
		},
		jdkProxies = @JdkProxyHint(typeNames = {
				"org.springframework.data.annotation.QueryAnnotation",
				"org.springframework.core.annotation.SynthesizedAnnotation" }
		),
		initialization = @InitializationHint(types = AbstractMappingContext.class, initTime = InitializationTime.BUILD)
	)
	 */

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {

		System.out.println("Spring data Runtime Hints");

		hints.reflection()
				.registerTypes(Arrays.asList(TypeReference.of(RepositoryFactoryBeanSupport.class),
						TypeReference.of(RepositoryFragmentsFactoryBean.class), TypeReference.of(RepositoryFragment.class),
						TypeReference.of(TransactionalRepositoryFactoryBeanSupport.class),
						TypeReference.of(QueryByExampleExecutor.class), TypeReference.of(MappingContext.class),
						TypeReference.of(RepositoryMetadata.class)), builder -> {
							builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
						});
		hints.reflection().registerTypes(
				Arrays.asList(TypeReference.of(Properties.class), TypeReference.of(BeanFactory.class),
						TypeReference.of(InputStreamSource[].class)),
				builder -> builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));

		hints.reflection().registerType(Throwable.class,
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS));

		hints.reflection().registerType(
				TypeReference.of("org.springframework.data.projection.SpelEvaluatingMethodInterceptor$TargetWrapper"),
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS));

		hints.proxies().registerJdkProxy(TypeReference.of("org.springframework.data.annotation.QueryAnnotation"),
				TypeReference.of("org.springframework.core.annotation.SynthesizedAnnotation"));

		hints.proxies().registerJdkProxy(TypeReference.of(AuditorAware.class), TypeReference.of(SpringProxy.class), TypeReference.of(Advised.class), TypeReference.of(DecoratingProxy.class));
	}
}
