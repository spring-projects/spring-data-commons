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
package org.springframework.data.domain.jaxb;

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.jaxb.SpringDataJaxb.PageRequestDto;
import org.springframework.data.domain.jaxb.SpringDataJaxb.SortDto;
import org.springframework.lang.Nullable;

/**
 * {@link XmlAdapter} to convert {@link Pageable} instances int a {@link PageRequestDto} and vice versa.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
class PageableAdapter extends XmlAdapter<PageRequestDto, Pageable> {

	/*
	 * (non-Javadoc)
	 * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
	 */
	@Nullable
	@Override
	public PageRequestDto marshal(@Nullable Pageable request) {

		if (request == null) {
			return null;
		}

		PageRequestDto dto = new PageRequestDto();

		SortDto sortDto = SortAdapter.INSTANCE.marshal(request.getSort());
		dto.orders = sortDto == null ? Collections.emptyList() : sortDto.orders;
		dto.page = request.getPageNumber();
		dto.size = request.getPageSize();

		return dto;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
	 */
	@Nonnull
	@Override
	public Pageable unmarshal(@Nullable PageRequestDto v) {

		if (v == null) {
			return Pageable.unpaged();
		}

		if (v.orders.isEmpty()) {
			return PageRequest.of(v.page, v.size);
		}

		SortDto sortDto = new SortDto();
		sortDto.orders = v.orders;

		return PageRequest.of(v.page, v.size, SortAdapter.INSTANCE.unmarshal(sortDto));
	}
}
