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
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.jaxb.SpringDataJaxb.PageDto;
import org.springframework.hateoas.Link;
import org.springframework.lang.Nullable;

/**
 * {@link XmlAdapter} to convert {@link Page} instances into {@link PageDto} instances and vice versa.
 *
 * @author Oliver Gierke
 */
public class PageAdapter extends XmlAdapter<PageDto, Page<Object>> {

	/*
	 * (non-Javadoc)
	 * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
	 */
	@Nullable
	@Override
	public PageDto marshal(@Nullable Page<Object> source) {

		if (source == null) {
			return null;
		}

		PageDto dto = new PageDto();
		dto.content = source.getContent();
		dto.add(getLinks(source));

		return dto;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
	 */
	@Nullable
	@Override
	public Page<Object> unmarshal(@Nullable PageDto v) {
		return null;
	}

	/**
	 * Return additional links that shall be added to the {@link PageDto}.
	 *
	 * @param source the source {@link Page}.
	 * @return
	 */
	protected List<Link> getLinks(Page<?> source) {
		return Collections.emptyList();
	}
}
