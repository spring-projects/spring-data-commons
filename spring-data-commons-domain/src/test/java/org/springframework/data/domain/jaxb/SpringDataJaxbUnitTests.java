/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.domain.jaxb;

import static org.assertj.core.api.Assertions.*;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.custommonkey.xmlunit.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;

/**
 * Unit test for custom JAXB conversions for Spring Data value objects.
 *
 * @author Oliver Gierke
 */
class SpringDataJaxbUnitTests {

	Marshaller marshaller;
	Unmarshaller unmarshaller;

	Sort sort = Sort.by(Direction.ASC, "firstname", "lastname");
	Pageable pageable = PageRequest.of(2, 15, sort);
	Resource resource = new ClassPathResource("pageable.xml", this.getClass());
	Resource schemaFile = new ClassPathResource("spring-data-jaxb.xsd", this.getClass());

	String reference = readFile(resource);

	@BeforeEach
	void setUp() throws Exception {

		var context = JAXBContext.newInstance("org.springframework.data.domain.jaxb");

		marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		unmarshaller = context.createUnmarshaller();
	}

	@Test
	void usesCustomTypeAdapterForPageRequests() throws Exception {

		var writer = new StringWriter();
		var wrapper = new Wrapper();
		wrapper.pageable = pageable;
		wrapper.sort = sort;
		wrapper.pageableWithoutSort = PageRequest.of(10, 20);
		marshaller.marshal(wrapper, writer);

		assertThat(new Diff(reference, writer.toString()).similar()).isTrue();
	}

	@Test
	void readsPageRequest() throws Exception {

		var result = unmarshaller.unmarshal(resource.getFile());

		assertThat(result).isInstanceOf(Wrapper.class);
		assertThat(((Wrapper) result).pageable).isEqualTo(pageable);
		assertThat(((Wrapper) result).sort).isEqualTo(sort);
	}

	@Test
	void writesPlainPage() throws Exception {

		var wrapper = new PageWrapper();
		var content = new Content();
		content.name = "Foo";
		wrapper.page = new PageImpl<>(Collections.singletonList(content));
		wrapper.pageWithLinks = new PageImpl<>(Collections.singletonList(content));

		marshaller.marshal(wrapper, new StringWriter());
	}

	private static String readFile(Resource resource) {

		try {

			var scanner = new Scanner(resource.getInputStream());
			var builder = new StringBuilder();

			while (scanner.hasNextLine()) {
				builder.append(scanner.nextLine()).append("\n");
			}

			scanner.close();

			return builder.toString();

		} catch (IOException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class Wrapper {

		@XmlElement(name = "page-request", namespace = SpringDataJaxb.NAMESPACE) Pageable pageable;

		@XmlElement(name = "page-request-without-sort", namespace = SpringDataJaxb.NAMESPACE) Pageable pageableWithoutSort;

		@XmlElement(name = "sort", namespace = SpringDataJaxb.NAMESPACE) Sort sort;
	}

	@XmlRootElement(name = "wrapper", namespace = SpringDataJaxb.NAMESPACE)
	static class PageWrapper {

		Page<Content> page;

		@XmlElement(name = "page-with-links") @XmlJavaTypeAdapter(LinkedPageAdapter.class) Page<Content> pageWithLinks;
	}

	@XmlRootElement
	static class Content {

		@XmlAttribute String name;
	}

	static class LinkedPageAdapter extends PageAdapter {

		@Override
		protected List<Link> getLinks(Page<?> source) {
			return Arrays.asList(Link.of(IanaLinkRelations.NEXT.value(), IanaLinkRelations.NEXT),
					Link.of(IanaLinkRelations.PREV.value(), IanaLinkRelations.PREV));
		}
	}
}
