/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.web;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link PagedResourcesAssembler}.
 * 
 * @author Oliver Gierke
 * @author Nick Williams
 */
public class PagedResourcesAssemblerUnitTests {

	HateoasPageableHandlerMethodArgumentResolver resolver = new HateoasPageableHandlerMethodArgumentResolver();

	@Before
	public void setUp() {
		WebTestUtils.initWebTest();
	}

	@Test
	public void addsNextLinkForFirstPage() {

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<Person>(resolver, null);
		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(0));

		assertThat(resources.getLink(Link.REL_PREVIOUS), is(nullValue()));
		assertThat(resources.getLink(Link.REL_NEXT), is(notNullValue()));
	}

	@Test
	public void addsPreviousAndNextLinksForMiddlePage() {

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<Person>(resolver, null);
		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(1));

		assertThat(resources.getLink(Link.REL_PREVIOUS), is(notNullValue()));
		assertThat(resources.getLink(Link.REL_NEXT), is(notNullValue()));
	}

	@Test
	public void addsPreviousLinkForLastPage() {

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<Person>(resolver, null);
		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(2));

		assertThat(resources.getLink(Link.REL_PREVIOUS), is(notNullValue()));
		assertThat(resources.getLink(Link.REL_NEXT), is(nullValue()));
	}

	@Test
	public void usesBaseUriIfConfigured() {

		UriComponents baseUri = UriComponentsBuilder.fromUriString("http://foo:9090").build();

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<Person>(resolver, baseUri);
		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(1));

		assertThat(resources.getLink(Link.REL_PREVIOUS).getHref(), startsWith(baseUri.toUriString()));
		assertThat(resources.getLink(Link.REL_NEXT).getHref(), startsWith(baseUri.toUriString()));
	}

	@Test
	public void usesCustomLinkProvided() {

		Link link = new Link("http://foo:9090", "rel");

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<Person>(resolver, null);
		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(1), link);

		assertThat(resources.getLink(Link.REL_PREVIOUS).getHref(), startsWith(link.getHref()));
		assertThat(resources.getLink(Link.REL_NEXT).getHref(), startsWith(link.getHref()));
	}

	/**
	 * @see DATACMNS-358
	 */
	@Test
	public void createsPagedResourcesForOneIndexedArgumentResolver() {

		resolver.setOneIndexedParameters(true);
		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<Person>(resolver, null);

		PageRequest request = new PageRequest(0, 1);
		Page<Person> page = new PageImpl<Person>(Collections.<Person> emptyList(), request, 0);

		assembler.toResource(page);
	}

	private static Page<Person> createPage(int index) {

		PageRequest request = new PageRequest(index, 1);
		return new PageImpl<Person>(Arrays.asList(new Person()), request, 3);
	}

	static class Person {

	}
}
