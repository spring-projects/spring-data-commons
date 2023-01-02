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
package org.springframework.data.repository.aot.hint;

import java.util.Arrays;
import java.util.Properties;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.InputStreamSource;
import org.springframework.data.domain.Example;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.core.support.RepositoryFragmentsFactoryBean;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.repository.query.FluentQuery.ReactiveFluentQuery;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.util.ReactiveWrappers;
import org.springframework.lang.Nullable;

/**
 * {@link RuntimeHintsRegistrar} holding required hints to bootstrap data repositories. <br />
 * Already registered via {@literal aot.factories}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.0
 */
class RepositoryRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {

		// repository infrastructure
		hints.reflection().registerTypes(Arrays.asList( //
				TypeReference.of(RepositoryFactoryBeanSupport.class), //
				TypeReference.of(RepositoryFragmentsFactoryBean.class), //
				TypeReference.of(RepositoryFragment.class), //
				TypeReference.of(TransactionalRepositoryFactoryBeanSupport.class), //
				TypeReference.of(Example.class), //
				TypeReference.of(QueryByExampleExecutor.class), //
				TypeReference.of(MappingContext.class), //
				TypeReference.of(RepositoryMetadata.class), //
				TypeReference.of(FluentQuery.class), //
				TypeReference.of(FetchableFluentQuery.class) //
		), builder -> {
			builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
		});

		if (ReactiveWrappers.PROJECT_REACTOR_PRESENT) {

			// repository infrastructure
			hints.reflection().registerTypes(Arrays.asList( //
					TypeReference.of(ReactiveFluentQuery.class), //
					TypeReference.of(ReactiveQueryByExampleExecutor.class)), //
					builder -> {
						builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
					});
		}

		// named queries
		hints.reflection().registerTypes(Arrays.asList( //
				TypeReference.of(Properties.class), //
				TypeReference.of(BeanFactory.class), //
				TypeReference.of(InputStreamSource[].class) //
		), builder -> builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));

		//
		hints.reflection().registerType(Throwable.class,
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS));

		// SpEL support
		hints.reflection().registerType(
				TypeReference.of("org.springframework.data.projection.SpelEvaluatingMethodInterceptor$TargetWrapper"),
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS));

		// annotated queries
		hints.proxies().registerJdkProxy( //
				TypeReference.of("org.springframework.data.annotation.QueryAnnotation"));
	}
}
