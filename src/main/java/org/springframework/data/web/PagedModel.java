/*
 * Copyright 2024-present the original author or authors.
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
package org.springframework.data.web;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Page;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO to build stable JSON representations of a Spring Data {@link Page}. It can either be selectively used in
 * controller methods by calling {@code new PagedModel<>(page)} or generally activated as representation model for
 * {@link org.springframework.data.domain.PageImpl} instances by setting
 * {@link org.springframework.data.web.config.EnableSpringDataWebSupport}'s {@code pageSerializationMode} to
 * {@link org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode#VIA_DTO}.
 *
 * @author Oliver Drotbohm
 * @author Greg Turnquist
 * @since 3.3
 */
public class PagedModel<T> {

	private final Page<T> page;

	/**
	 * Creates a new {@link PagedModel} for the given {@link Page}.
	 *
	 * @param page must not be {@literal null}.
	 */
	public PagedModel(Page<T> page) {

		Assert.notNull(page, "Page must not be null");

		this.page = page;
	}

	@JsonProperty
	public List<T> getContent() {
		return page.getContent();
	}

	@JsonProperty("page")
	public PageMetadata getMetadata() {
		return new PageMetadata(page.getSize(), page.getNumber(), page.getTotalElements(),
				page.getTotalPages());
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof PagedModel<?> that)) {
			return false;
		}

		return Objects.equals(this.page, that.page);
	}

	@Override
	public int hashCode() {
		return Objects.hash(page);
	}

	public record PageMetadata(long size, long number, long totalElements, long totalPages) {

		public PageMetadata {
			Assert.isTrue(size > -1, "Size must not be negative!");
			Assert.isTrue(number > -1, "Number must not be negative!");
			Assert.isTrue(totalElements > -1, "Total elements must not be negative!");
			Assert.isTrue(totalPages > -1, "Total pages must not be negative!");
		}
	}
}
