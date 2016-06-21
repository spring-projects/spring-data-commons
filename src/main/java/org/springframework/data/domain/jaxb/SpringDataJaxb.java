/*
 * Copyright 2012 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

/**
 * Helper class containing utility methods to implement JAXB {@link XmlAdapter}s as well as the DTO types to be
 * marshalled by JAXB.
 * 
 * @author Oliver Gierke
 */
public abstract class SpringDataJaxb {

	public static final String NAMESPACE = "http://www.springframework.org/schema/data/jaxb";

	private SpringDataJaxb() {}

	/**
	 * The DTO for {@link Pageable}s/{@link PageRequest}s.
	 * 
	 * @author Oliver Gierke
	 */
	@XmlRootElement(name = "page-request", namespace = NAMESPACE)
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class PageRequestDto {

		@XmlAttribute int page, size;
		@XmlElement(name = "order", namespace = NAMESPACE) List<OrderDto> orders = new ArrayList<>();
	}

	/**
	 * The DTO for {@link Sort}.
	 * 
	 * @author Oliver Gierke
	 */
	@XmlRootElement(name = "sort", namespace = NAMESPACE)
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class SortDto {

		@XmlElement(name = "order", namespace = SpringDataJaxb.NAMESPACE) List<OrderDto> orders = new ArrayList<>();
	}

	/**
	 * The DTO for {@link Order}.
	 * 
	 * @author Oliver Gierke
	 */
	@XmlRootElement(name = "order", namespace = NAMESPACE)
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class OrderDto {

		@XmlAttribute String property;
		@XmlAttribute Direction direction;
	}

	/**
	 * The DTO for {@link Page}.
	 * 
	 * @author Oliver Gierke
	 */
	@XmlRootElement(name = "page", namespace = NAMESPACE)
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class PageDto extends ResourceSupport {

		@XmlAnyElement @XmlElementWrapper(name = "content") List<Object> content;
	}

	/**
	 * Unmarshals each element of the given {@link Collection} using the given {@link XmlAdapter}.
	 * 
	 * @param source
	 * @param adapter must not be {@literal null}.
	 * @return
	 * @throws Exception
	 */
	public static <T, S> List<T> unmarshal(Collection<S> source, XmlAdapter<S, T> adapter) {

		Assert.notNull(adapter);

		if (source == null || source.isEmpty()) {
			return Collections.emptyList();
		}

		List<T> result = new ArrayList<>(source.size());

		for (S element : source) {
			try {
				result.add(adapter.unmarshal(element));
			} catch (Exception o_O) {
				throw new RuntimeException(o_O);
			}
		}
		return result;
	}

	/**
	 * Marshals each of the elements of the given {@link Iterable} using the given {@link XmlAdapter}.
	 * 
	 * @param source
	 * @param adapter must not be {@literal null}.
	 * @return
	 * @throws Exception
	 */
	public static <T, S> List<S> marshal(Iterable<T> source, XmlAdapter<S, T> adapter) {

		Assert.notNull(adapter);

		if (source == null) {
			return Collections.emptyList();
		}

		List<S> result = new ArrayList<>();

		for (T element : source) {
			try {
				result.add(adapter.marshal(element));
			} catch (Exception o_O) {
				throw new RuntimeException(o_O);
			}
		}

		return result;
	}
}
