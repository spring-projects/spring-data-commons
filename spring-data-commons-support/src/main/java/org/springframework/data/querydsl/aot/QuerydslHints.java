/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.data.querydsl.aot;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.data.util.ReactiveWrappers;

import com.querydsl.core.types.Predicate;

/**
 * {@link RuntimeHintsRegistrar} holding required hints to bootstrap Querydsl repositories. <br />
 * Already registered via {@literal aot.factories}.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
class QuerydslHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {

		if (QuerydslUtils.QUERY_DSL_PRESENT) {

			// repository infrastructure
			hints.reflection().registerTypes(Arrays.asList( //
					TypeReference.of(Predicate.class), //
					TypeReference.of(QuerydslPredicateExecutor.class)), builder -> {
						builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
					});

			if (ReactiveWrappers.PROJECT_REACTOR_PRESENT) {

				// repository infrastructure
				hints.reflection().registerTypes(Arrays.asList( //
						TypeReference.of(ReactiveQuerydslPredicateExecutor.class)), builder -> {
							builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
						});
			}
		}
	}
}
