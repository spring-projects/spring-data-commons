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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Unit tests for SortExtensions.
 *
 * @author Yejun Ho
 */
class SortExtensionsTests {

    @Test // GH-3445
    fun `KProperty#asc() extension should create ascending Java Sort`() {
        assertThat(Book::title.asc()).isEqualTo(Sort.by(Sort.Direction.ASC, Book::title.name))
    }

    @Test // GH-3445
    fun `KProperty#desc() extension should create descending Java Sort`() {
        assertThat(Book::title.desc()).isEqualTo(Sort.by(Sort.Direction.DESC, Book::title.name))
    }

    class Book(val title: String)
}
