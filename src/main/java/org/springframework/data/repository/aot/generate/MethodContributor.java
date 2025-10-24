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

import org.jspecify.annotations.Nullable;

import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.CodeBlock;
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
	private final QueryMetadata metadata;

	MethodContributor(M queryMethod, QueryMetadata metadata) {
		this.queryMethod = queryMethod;
		this.metadata = metadata;
	}

	/**
	 * Creates a new builder to build a {@code MethodContributor}.
	 *
	 * @param queryMethod the query method to be used.
	 * @return the new builder.
	 * @param <M> query method type.
	 */
	public static <M extends QueryMethod> QueryMethodMetadataContributorBuilder<M> forQueryMethod(M queryMethod) {

		return new QueryMethodMetadataContributorBuilder<>() {

			@Override
			public MethodContributor<M> metadataOnly(QueryMetadata metadata) {
				return new MetadataContributor<>(queryMethod, metadata);
			}


			@Override
			public QueryMethodContributorBuilder<M> withMetadata(QueryMetadata metadata) {

				return contribution -> new GeneratedMethodContributor<>(queryMethod, metadata, b -> {
					b.contribute(contribution::contribute);
				});
			}

		};
	}

	public M getQueryMethod() {
		return queryMethod;
	}

	public QueryMetadata getMetadata() {
		return metadata;
	}

	/**
	 * @return whether {@code MethodContributor} can contribute a {@link MethodSpec} implementing the actual query method.
	 */
	public boolean contributesMethodSpec() {
		return false;
	}

	/**
	 * Contribute the actual method specification to be added to the repository fragment.
	 *
	 * @param context generation context.
	 * @return
	 */
	public abstract @Nullable MethodSpec contribute(AotQueryMethodGenerationContext context);

	/**
	 * Initial builder for a query method contributor. This builder allows returning a {@link MethodContributor} using
	 * metadata-only (i.e. no code contribution) or a {@link QueryMethodContributorBuilder} accepting code contributions.
	 *
	 * @param <M> query method type.
	 */
	public interface QueryMethodMetadataContributorBuilder<M extends QueryMethod> {

		/**
		 * Terminal method accepting {@link QueryMetadata} to build the {@link MethodContributor}.
		 *
		 * @param metadata the query metadata describing queries used by the query method.
		 * @return the method contributor to use.
		 */
		MethodContributor<M> metadataOnly(QueryMetadata metadata);

		/**
		 * Builder method accepting {@link QueryMetadata} to enrich query method metadata.
		 *
		 * @param metadata the query metadata describing queries used by the query method.
		 * @return the method contributor builder.
		 */
		QueryMethodContributorBuilder<M> withMetadata(QueryMetadata metadata);

	}

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
		MethodContributor<M> contribute(RepositoryMethodContribution contribution);

	}

	/**
	 * AOT contribution from a {@link AotRepositoryMethodBuilder} used to contribute a repository query method body.
	 */
	public interface RepositoryMethodContribution {

		CodeBlock contribute(AotQueryMethodGenerationContext context);

	}

	/**
	 * Customizer for a contributed AOT repository query method.
	 */
	public interface RepositoryMethodCustomizer {

		void customize(AotQueryMethodGenerationContext context, CodeBlock.Builder builder);

	}

	private static class MetadataContributor<M extends QueryMethod> extends MethodContributor<M> {

		private MetadataContributor(M queryMethod, QueryMetadata metadata) {
			super(queryMethod, metadata);
		}

		@Override
		public @Nullable MethodSpec contribute(AotQueryMethodGenerationContext context) {
			return null;
		}

	}

	private static class GeneratedMethodContributor<M extends QueryMethod> extends MethodContributor<M> {

		private final Consumer<AotRepositoryMethodBuilder> builderConsumer;

		private GeneratedMethodContributor(M queryMethod, QueryMetadata metadata,
				Consumer<AotRepositoryMethodBuilder> builderConsumer) {
			super(queryMethod, metadata);
			this.builderConsumer = builderConsumer;
		}

		@Override
		public boolean contributesMethodSpec() {
			return true;
		}

		@Override
		public MethodSpec contribute(AotQueryMethodGenerationContext context) {

			AotRepositoryMethodBuilder builder = new AotRepositoryMethodBuilder(context);
			builderConsumer.accept(builder);
			return builder.buildMethod();
		}

	}

}
