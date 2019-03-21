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
package org.springframework.data.spel.spi;

import org.springframework.data.support.SubscriberContextAware;

/**
 * Extension to {@link EvaluationContextExtension} that wants to be notified about the subscriber
 * {@link reactor.util.context.Context} to provide a contextualized version of the extension instance.
 *
 * @author Mark Paluch
 * @since 2.1
 */
public interface SubscriberContextAwareExtension extends SubscriberContextAware<EvaluationContextExtension> {}
