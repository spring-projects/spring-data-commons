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
package org.springframework.data.web;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Slice;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO to build stable JSON representations of a Spring Data {@link Slice}. It can either be selectively used in
 * controller methods by calling {@code new SlicedModel<>(slice)} or generally activated as representation model for
 * {@link org.springframework.data.domain.SliceImpl} instances by setting
 * {@link org.springframework.data.web.config.EnableSpringDataWebSupport}'s {@code pageSerializationMode} to
 * {@link org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode#VIA_DTO}.
 *
 * @author Adrien Caubel
 * @since 4.2
 */
public class SlicedModel<T> {

	private final Slice<T> slice;

	/**
	 * Creates a new {@link SlicedModel} for the given {@link Slice}.
	 *
	 * @param slice must not be {@literal null}.
	 */
	public SlicedModel(Slice<T> slice) {

		Assert.notNull(slice, "Slice must not be null");

		this.slice = slice;
	}

	@JsonProperty
	public List<T> getContent() {
		return slice.getContent();
	}

	@JsonProperty("page")
	public SliceMetadata getMetadata() {
		return new SliceMetadata(slice.getSize(), slice.getNumber(), slice.hasNext());
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof SlicedModel<?> that)) {
			return false;
		}

		return Objects.equals(this.slice, that.slice);
	}

	@Override
	public int hashCode() {
		return Objects.hash(slice);
	}

	public record SliceMetadata(long size, long number, boolean hasNext) {

		public SliceMetadata {
			Assert.isTrue(size > -1, "Size must not be negative!");
			Assert.isTrue(number > -1, "Number must not be negative!");
		}
	}
}
