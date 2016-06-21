/*
 * Copyright 2012-2014 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.custommonkey.xmlunit.Diff;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.Link;

/**
 * Unit test for custom JAXB conversions for Spring Data value objects.
 * 
 * @author Oliver Gierke
 */
public class SpringDataJaxbUnitTests {

	Marshaller marshaller;
	Unmarshaller unmarshaller;

	Sort sort = new Sort(Direction.ASC, "firstname", "lastname");
	Pageable pageable = PageRequest.of(2, 15, sort);
	Resource resource = new ClassPathResource("pageable.xml", this.getClass());
	Resource schemaFile = new ClassPathResource("spring-data-jaxb.xsd", this.getClass());

	String reference = readFile(resource);

	@Before
	public void setUp() throws Exception {

		JAXBContext context = JAXBContext.newInstance("org.springframework.data.domain.jaxb");

		marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		unmarshaller = context.createUnmarshaller();
	}

	@Test
	public void usesCustomTypeAdapterForPageRequests() throws Exception {

		StringWriter writer = new StringWriter();
		Wrapper wrapper = new Wrapper();
		wrapper.pageable = pageable;
		wrapper.sort = sort;
		wrapper.pageableWithoutSort = PageRequest.of(10, 20);
		marshaller.marshal(wrapper, writer);

		assertThat(new Diff(reference, writer.toString()).similar()).isTrue();
	}

	@Test
	public void readsPageRequest() throws Exception {

		Object result = unmarshaller.unmarshal(resource.getFile());

		assertThat(result).isInstanceOf(Wrapper.class);
		assertThat(((Wrapper) result).pageable).isEqualTo(pageable);
		assertThat(((Wrapper) result).sort).isEqualTo(sort);
	}

	@Test
	public void writesPlainPage() throws Exception {

		PageWrapper wrapper = new PageWrapper();
		Content content = new Content();
		content.name = "Foo";
		wrapper.page = new PageImpl<>(Arrays.asList(content));
		wrapper.pageWithLinks = new PageImpl<>(Arrays.asList(content));

		marshaller.marshal(wrapper, new StringWriter());
	}

	private static String readFile(Resource resource) {

		try {

			Scanner scanner = new Scanner(resource.getInputStream());
			StringBuilder builder = new StringBuilder();

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
			return Arrays.asList(new Link(Link.REL_NEXT, "next"), new Link(Link.REL_PREVIOUS, "previous"));
		}
	}
}
