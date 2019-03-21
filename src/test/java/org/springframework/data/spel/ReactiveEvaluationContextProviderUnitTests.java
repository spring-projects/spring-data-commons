/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.spel;

import static org.assertj.core.api.Assertions.*;

import reactor.util.context.Context;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.SubscriberContextAwareExtension;

/**
 * Unit tests for {@link ReactiveEvaluationContextProvider}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactiveEvaluationContextProviderUnitTests {

	@Test // DATACMNS-1108
	public void shouldContextualizeExtension() {

		ContextualExtension extension = new ContextualExtension(null);
		ReactiveEvaluationContextProvider provider = new ReactiveEvaluationContextProvider(
				Collections.singleton(extension));

		Context context = Context.of("foo", "bar");

		Collection<? extends EvaluationContextExtension> extensions = provider.withSubscriberContext(context)
				.getExtensions();

		assertThat(extensions).hasSize(1);

		assertThat(extensions.iterator().next().getProperties()).containsEntry("foo", "bar");
	}

	static class ContextualExtension implements EvaluationContextExtension, SubscriberContextAwareExtension {

		private final Context context;

		ContextualExtension(Context context) {
			this.context = context;
		}

		@Override
		public String getExtensionId() {
			return "foo";
		}

		@Override
		public Map<String, Object> getProperties() {
			return context.stream().collect(Collectors.toMap(it -> (String) it.getKey(), Entry::getValue));
		}

		@Override
		public EvaluationContextExtension withSubscriberContext(Context context) {
			return new ContextualExtension(context);
		}
	}
}
