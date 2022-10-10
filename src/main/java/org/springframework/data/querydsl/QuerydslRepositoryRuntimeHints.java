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
package org.springframework.data.querydsl;

import com.querydsl.core.types.Predicate;
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
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Properties;

/**
 * {@link RuntimeHintsRegistrar} holding required hints to bootstrap Querydsl repositories. <br />
 * Already registered via {@literal aot.factories}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.0
 */
class QuerydslRepositoryRuntimeHints implements RuntimeHintsRegistrar {

	private static final boolean PROJECT_REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.publisher.Flux",
			QuerydslRepositoryRuntimeHints.class.getClassLoader());

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {

		if (QuerydslUtils.QUERY_DSL_PRESENT) {

			// repository infrastructure
			hints.reflection().registerTypes(Arrays.asList( //
					TypeReference.of(Predicate.class), //
					TypeReference.of(QuerydslPredicateExecutor.class)), builder -> {
						builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
					});

			if (PROJECT_REACTOR_PRESENT) {
				// repository infrastructure
				hints.reflection().registerTypes(Arrays.asList( //
						TypeReference.of(ReactiveQuerydslPredicateExecutor.class)), builder -> {
							builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
						});
			}
		}
	}
}
