/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.domain;

import reactor.core.publisher.Mono;

/**
 * Interface for components that are aware of the application's current auditor. This will be some kind of user mostly.
 *
 * @param <T> the type of the auditing instance.
 * @author Mark Paluch
 * @since 2.4
 */
public interface ReactiveAuditorAware<T> {

	/**
	 * Returns a {@link Mono} publishing the current auditor of the application.
	 *
	 * @return the {@link Mono} emitting the current auditor, or an empty one, if the auditor is considered to be unknown.
	 */
	Mono<T> getCurrentAuditor();

}
