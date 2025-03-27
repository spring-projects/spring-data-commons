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
package org.springframework.data.repository.aot.generate;

import java.util.function.Consumer;

import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.MethodSpec;

/**
 * Strategy for contributing AOT repository methods by looking introspecting query methods.
 *
 * @param <M> query method type.
 * @author Mark Paluch
 * @since 4.0
 */
public abstract class MethodContributor<M extends QueryMethod> {

	private final M queryMethod;

	private MethodContributor(M queryMethod) {
		this.queryMethod = queryMethod;
	}

	/**
	 * Creates a new builder to build a {@code MethodContributor}.
	 *
	 * @param queryMethod the query method to be used.
	 * @return the new builder.
	 * @param <M> query method type.
	 */
	public static <M extends QueryMethod> QueryMethodContributorBuilder<M> forQueryMethod(M queryMethod) {

		return builderConsumer -> new MethodContributor<>(queryMethod) {

			@Override
			public MethodSpec contribute(AotQueryMethodGenerationContext context) {
				AotRepositoryMethodBuilder builder = new AotRepositoryMethodBuilder(context);
				builderConsumer.accept(builder);
				return builder.buildMethod();
			}
		};
	}

	public M getQueryMethod() {
		return queryMethod;
	}

	/**
	 * Contribute the actual method specification to be added to the repository fragment.
	 *
	 * @param context generation context.
	 * @return
	 */
	public abstract MethodSpec contribute(AotQueryMethodGenerationContext context);

	/**
	 * Builder for a query method contributor.
	 *
	 * @param <M> query method type.
	 */
	public interface QueryMethodContributorBuilder<M extends QueryMethod> {

		/**
		 * Terminal method accepting a consumer that uses {@link AotRepositoryMethodBuilder} to build the method.
		 *
		 * @param contribution the method contribution that provides the method to be added to the repository.
		 * @return the method contributor to use.
		 */
		default MethodContributor<M> contribute(AotRepositoryMethodBuilder.RepositoryMethodContribution contribution) {
			return using(builder -> builder.contribute(contribution));
		}

		/**
		 * Terminal method accepting a consumer that uses {@link AotRepositoryMethodBuilder} to build the method.
		 *
		 * @param builderConsumer consumer method being provided with the {@link AotRepositoryMethodBuilder} that provides
		 *          the method to be added to the repository.
		 * @return the method contributor to use.
		 */
		MethodContributor<M> using(Consumer<AotRepositoryMethodBuilder> builderConsumer);

	}

}
