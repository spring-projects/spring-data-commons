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
package org.springframework.data.auditing;

import java.time.temporal.TemporalAccessor;
import java.util.Optional;

/**
 * SPI to calculate the current time to be used when auditing.
 *
 * @author Oliver Gierke
 * @since 1.5
 */
public interface DateTimeProvider {

	/**
	 * Returns the current time to be used as modification or creation date.
	 *
	 * @return
	 */
	Optional<TemporalAccessor> getNow();
}
