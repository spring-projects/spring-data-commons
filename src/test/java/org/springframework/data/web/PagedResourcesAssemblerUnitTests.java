/*
 * Copyright 2013-2017 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link PagedResourcesAssembler}.
 * 
 * @author Oliver Gierke
 * @author Nick Williams
 */
public class PagedResourcesAssemblerUnitTests {

	static final Pageable PAGEABLE = new PageRequest(0, 20);
	static final Page<Person> EMPTY_PAGE = new PageImpl<Person>(Collections.<Person> emptyList(), PAGEABLE, 0);
	static final Matcher<String> NO_TEMPLATE_VARIABLES = allOf(not(containsString("{")), not(containsString("}")));

	HateoasPageableHandlerMethodArgumentResolver resolver = new HateoasPageableHandlerMethodArgumentResolver();
	PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<Person>(resolver, null);

	@Before
	public void setUp() {
		WebTestUtils.initWebTest();
	}

	@Test
	public void addsNextLinkForFirstPage() {

		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(0));

		assertThat(resources.getLink(Link.REL_PREVIOUS), is(nullValue()));
		assertThat(resources.getLink(Link.REL_SELF), is(notNullValue()));
		assertThat(resources.getLink(Link.REL_NEXT), is(notNullValue()));
	}

	@Test
	public void addsPreviousAndNextLinksForMiddlePage() {

		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(1));

		assertThat(resources.getLink(Link.REL_PREVIOUS), is(notNullValue()));
		assertThat(resources.getLink(Link.REL_SELF), is(notNullValue()));
		assertThat(resources.getLink(Link.REL_NEXT), is(notNullValue()));
	}

	@Test
	public void addsPreviousLinkForLastPage() {

		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(2));

		assertThat(resources.getLink(Link.REL_PREVIOUS), is(notNullValue()));
		assertThat(resources.getLink(Link.REL_SELF), is(notNullValue()));
		assertThat(resources.getLink(Link.REL_NEXT), is(nullValue()));
	}

	@Test
	public void usesBaseUriIfConfigured() {

		UriComponents baseUri = UriComponentsBuilder.fromUriString("http://foo:9090").build();

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<Person>(resolver, baseUri);
		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(1));

		assertThat(resources.getLink(Link.REL_PREVIOUS).getHref(), startsWith(baseUri.toUriString()));
		assertThat(resources.getLink(Link.REL_SELF), is(notNullValue()));
		assertThat(resources.getLink(Link.REL_NEXT).getHref(), startsWith(baseUri.toUriString()));
	}

	@Test
	public void usesCustomLinkProvided() {

		Link link = new Link("http://foo:9090", "rel");

		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(1), link);

		assertThat(resources.getLink(Link.REL_PREVIOUS).getHref(), startsWith(link.getHref()));
		assertThat(resources.getLink(Link.REL_SELF), is(notNullValue()));
		assertThat(resources.getLink(Link.REL_NEXT).getHref(), startsWith(link.getHref()));
	}

	@Test // DATACMNS-358
	public void createsPagedResourcesForOneIndexedArgumentResolver() {

		resolver.setOneIndexedParameters(true);

		AbstractPageRequest request = new PageRequest(0, 1);
		Page<Person> page = new PageImpl<Person>(Collections.<Person> emptyList(), request, 0);

		assembler.toResource(page);
	}

	@Test // DATACMNS-418, DATACMNS-515
	public void createsACanonicalLinkWithoutTemplateParameters() {

		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(1));

		Link selfLink = resources.getLink(Link.REL_SELF);
		assertThat(selfLink.getHref(), NO_TEMPLATE_VARIABLES);
	}

	@Test // DATACMNS-418
	public void invokesCustomElementResourceAssembler() {

		PersonResourceAssembler personAssembler = new PersonResourceAssembler();

		PagedResources<PersonResource> resources = assembler.toResource(createPage(0), personAssembler);

		assertThat(resources.hasLink(Link.REL_SELF), is(true));
		assertThat(resources.hasLink(Link.REL_NEXT), is(true));
		Collection<PersonResource> content = resources.getContent();
		assertThat(content, hasSize(1));
		assertThat(content.iterator().next().name, is("Dave"));
	}

	@Test // DATAMCNS-563
	public void createsPaginationLinksForOneIndexedArgumentResolverCorrectly() {

		HateoasPageableHandlerMethodArgumentResolver argumentResolver = new HateoasPageableHandlerMethodArgumentResolver();
		argumentResolver.setOneIndexedParameters(true);

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<Person>(argumentResolver, null);
		PagedResources<Resource<Person>> resource = assembler.toResource(createPage(1));

		assertThat(resource.hasLink("prev"), is(true));
		assertThat(resource.hasLink("next"), is(true));

		assertThat(getQueryParameters(resource.getLink("prev")), hasEntry("page", "1"));
		assertThat(getQueryParameters(resource.getLink("next")), hasEntry("page", "3"));
	}

	@Test // DATACMNS-515
	public void generatedLinksShouldNotBeTemplated() {

		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(1));

		assertThat(resources.getLink(Link.REL_SELF).getHref(), NO_TEMPLATE_VARIABLES);
		assertThat(resources.getLink(Link.REL_NEXT).getHref(), endsWith("?page=2&size=1"));
		assertThat(resources.getLink(Link.REL_PREVIOUS).getHref(), endsWith("?page=0&size=1"));
	}

	@Test // DATACMNS-699
	public void generatesEmptyPagedResourceWithEmbeddedWrapper() {

		PagedResources<?> result = assembler.toEmptyResource(EMPTY_PAGE, Person.class, null);

		Collection<?> content = result.getContent();
		assertThat(content, hasSize(1));

		Object element = content.iterator().next();
		assertThat(element, is(instanceOf(EmbeddedWrapper.class)));
		assertThat(((EmbeddedWrapper) element).getRelTargetType(), is(typeCompatibleWith(Person.class)));
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-699
	public void emptyPageCreatorRejectsPageWithContent() {
		assembler.toEmptyResource(createPage(1), Person.class, null);
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-699
	public void emptyPageCreatorRejectsNullType() {
		assembler.toEmptyResource(EMPTY_PAGE, null, null);
	}

	@Test // DATACMNS-701
	public void addsFirstAndLastLinksForMultiplePages() {

		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(1));

		assertThat(resources.getLink(Link.REL_FIRST).getHref(), endsWith("?page=0&size=1"));
		assertThat(resources.getLink(Link.REL_LAST).getHref(), endsWith("?page=2&size=1"));
	}

	@Test // DATACMNS-701
	public void addsFirstAndLastLinksForFirstPage() {

		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(0));

		assertThat(resources.getLink(Link.REL_FIRST).getHref(), endsWith("?page=0&size=1"));
		assertThat(resources.getLink(Link.REL_LAST).getHref(), endsWith("?page=2&size=1"));
	}

	@Test // DATACMNS-701
	public void addsFirstAndLastLinksForLastPage() {

		PagedResources<Resource<Person>> resources = assembler.toResource(createPage(2));

		assertThat(resources.getLink(Link.REL_FIRST).getHref(), endsWith("?page=0&size=1"));
		assertThat(resources.getLink(Link.REL_LAST).getHref(), endsWith("?page=2&size=1"));
	}

	@Test // DATACMNS-701
	public void alwaysAddsFirstAndLastLinkIfConfiguredTo() {

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<Person>(resolver, null);
		assembler.setForceFirstAndLastRels(true);

		PagedResources<Resource<Person>> resources = assembler.toResource(EMPTY_PAGE);

		assertThat(resources.getLink(Link.REL_FIRST).getHref(), endsWith("?page=0&size=20"));
		assertThat(resources.getLink(Link.REL_LAST).getHref(), endsWith("?page=0&size=20"));
	}

	@Test // DATACMNS-802
	public void usesCustomPagedResources() {

		ResourceAssembler<Page<Person>, PagedResources<Resource<Person>>> assembler = new CustomPagedResourcesAssembler<Person>(
				resolver, null);

		assertThat(assembler.toResource(EMPTY_PAGE), is(instanceOf(CustomPagedResources.class)));
	}

	@Test // DATACMNS-1042
	public void selfLinkContainsCoordinatesForCurrentPage() {

		PagedResources<Resource<Person>> resource = assembler.toResource(createPage(0));

		assertThat(resource.getLink(Link.REL_SELF).getHref(), endsWith("?page=0&size=1"));
	}

	private static Page<Person> createPage(int index) {

		AbstractPageRequest request = new PageRequest(index, 1);

		Person person = new Person();
		person.name = "Dave";

		return new PageImpl<Person>(Arrays.asList(person), request, 3);
	}

	private static Map<String, String> getQueryParameters(Link link) {

		UriComponents uriComponents = UriComponentsBuilder.fromUri(URI.create(link.expand().getHref())).build();
		return uriComponents.getQueryParams().toSingleValueMap();
	}

	static class Person {
		String name;
	}

	static class PersonResource extends ResourceSupport {
		String name;
	}

	static class PersonResourceAssembler implements ResourceAssembler<Person, PersonResource> {

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.ResourceAssembler#toResource(java.lang.Object)
		 */
		@Override
		public PersonResource toResource(Person entity) {
			PersonResource resource = new PersonResource();
			resource.name = entity.name;
			return resource;
		}
	}

	static class CustomPagedResourcesAssembler<T> extends PagedResourcesAssembler<T> {

		public CustomPagedResourcesAssembler(HateoasPageableHandlerMethodArgumentResolver resolver, UriComponents baseUri) {
			super(resolver, baseUri);
		}

		@Override
		protected <R extends ResourceSupport, S> PagedResources<R> createPagedResource(List<R> resources,
				PageMetadata metadata, Page<S> page) {
			return new CustomPagedResources<R>(resources, metadata);
		}
	}

	static class CustomPagedResources<R extends ResourceSupport> extends PagedResources<R> {

		public CustomPagedResources(Collection<R> content, PageMetadata metadata) {
			super(content, metadata);
		}
	}
}
