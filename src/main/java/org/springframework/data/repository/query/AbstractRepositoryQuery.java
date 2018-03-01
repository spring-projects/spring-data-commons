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

package org.springframework.data.repository.query;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.data.repository.Repository;

/**
 * Abstract base class encapsulating functionality common to all {@link RepositoryQuery} implementations,
 * such as providing support for query post processing.
 *
 * @author John Blum
 * @see org.springframework.data.repository.Repository
 * @see org.springframework.data.repository.query.RepositoryQuery
 * @since 1.0.0
 */
@SuppressWarnings({ "unused", "unchecked" })
public abstract class AbstractRepositoryQuery<QUERY> implements RepositoryQuery {

	private QueryPostProcessor<?, QUERY> queryPostProcessor = ProvidedQueryPostProcessors.IDENTITY;

	/**
	 * Returns a reference to the composed {@link QueryPostProcessor QueryPostProcessors}, which are applied
	 * to {@link QUERY queries} before execution.
	 *
	 * @return a reference to the composed {@link QueryPostProcessor QueryPostProcessors}.
	 * @see org.springframework.data.repository.query.QueryPostProcessor
	 */
	protected QueryPostProcessor<?, QUERY> getQueryPostProcessor() {
		return this.queryPostProcessor;
	}

	/**
	 * Registers the given {@link QueryPostProcessor} to use for processing {@link QUERY queries}
	 * generated from {@link Repository} {@link QueryMethod query methods}.
	 *
	 * Registration always links the given {@link QueryPostProcessor} to the end of the processing chain
	 * of previously registered {@link QueryPostProcessor QueryPostProcessors}.  In other words, the given
	 * {@link QueryPostProcessor} argument will process {@link QUERY queries} only after all
	 * {@link QueryPostProcessor QueryPostProcess} registered before it.
	 *
	 * @param <T> {@link Class sub-type} of {@link Repository}.
	 * @param queryPostProcessor {@link QueryPostProcessor} to register.
	 * @return this {@link RepositoryQuery}.
	 * @see org.springframework.data.repository.query.QueryPostProcessor#processBefore(QueryPostProcessor)
	 */
	public <T extends RepositoryQuery> T register(QueryPostProcessor<?, QUERY> queryPostProcessor) {
		this.queryPostProcessor = this.queryPostProcessor.processBefore(queryPostProcessor);
		return (T) this;
	}

	public enum ProvidedQueryPostProcessors implements QueryPostProcessor {

		IDENTITY {

			@Override
			public int getOrder() {
				return Ordered.HIGHEST_PRECEDENCE;
			}

			@Override
			public Object postProcess(QueryMethod queryMethod, Object query, Object... arguments) {
				return query;
			}
		},

		LOGGING {

			private final Logger logger =
				LoggerFactory.getLogger(ProvidedQueryPostProcessors.class.getName().concat(".LOGGER"));

			@Override
			public Object postProcess(QueryMethod queryMethod, Object query, Object... arguments) {

				if (this.logger.isTraceEnabled()) {
					this.logger.trace("Executing query [{}] with arguments [{}] for query method [{}]",
						query, Arrays.toString(arguments), queryMethod.getName());
				}

				return query;
			}
		}
	}
}
