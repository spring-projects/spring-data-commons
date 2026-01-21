/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.domain

import org.springframework.data.mapping.toDotPath
import kotlin.reflect.KProperty

/**
 * Creates an ascending [Sort] based on this [KProperty].
 *
 * @author Yejun Ho
 * @since 4.0.3
 */
fun KProperty<*>.asc(): Sort =
    Sort.by(Sort.Direction.ASC, this.toDotPath())

/**
 * Creates a descending [Sort] based on this [KProperty].
 *
 * @author Yejun Ho
 * @since 4.0.3
 */
fun KProperty<*>.desc(): Sort =
    Sort.by(Sort.Direction.DESC, this.toDotPath())