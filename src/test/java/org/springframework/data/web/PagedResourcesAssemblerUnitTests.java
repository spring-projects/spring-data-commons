/*
 * Copyright 2013-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link PagedResourcesAssembler}.
 *
 * @author Oliver Gierke
 * @author Nick Williams
 * @author Marcel Overdijk
 */
public class PagedResourcesAssemblerUnitTests {

	static final Pageable PAGEABLE = PageRequest.of(0, 20);
	static final Page<Person> EMPTY_PAGE = new PageImpl<>(Collections.emptyList(), PAGEABLE, 0);

	HateoasPageableHandlerMethodArgumentResolver resolver = new HateoasPageableHandlerMethodArgumentResolver();
	PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<>(resolver, null);

	@Before
	public void setUp() {
		WebTestUtils.initWebTest();
	}

	@Test
	public void addsNextLinkForFirstPage() {

		PagedModel<EntityModel<Person>> resources = assembler.toModel(createPage(0));

		assertThat(resources.getLink(IanaLinkRelations.PREV)).isEmpty();
		assertThat(resources.getLink(IanaLinkRelations.SELF)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.NEXT)).isNotEmpty();
	}

	@Test
	public void addsPreviousAndNextLinksForMiddlePage() {

		PagedModel<EntityModel<Person>> resources = assembler.toModel(createPage(1));

		assertThat(resources.getLink(IanaLinkRelations.PREV)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.SELF)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.NEXT)).isNotEmpty();
	}

	@Test
	public void addsPreviousLinkForLastPage() {

		PagedModel<EntityModel<Person>> resources = assembler.toModel(createPage(2));

		assertThat(resources.getLink(IanaLinkRelations.PREV)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.SELF)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.NEXT)).isEmpty();
	}

	@Test
	public void usesBaseUriIfConfigured() {

		UriComponents baseUri = UriComponentsBuilder.fromUriString("http://foo:9090").build();

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<>(resolver, baseUri);
		PagedModel<EntityModel<Person>> resources = assembler.toModel(createPage(1));

		assertThat(resources.getRequiredLink(IanaLinkRelations.PREV).getHref()).startsWith(baseUri.toUriString());
		assertThat(resources.getRequiredLink(IanaLinkRelations.SELF)).isNotNull();
		assertThat(resources.getRequiredLink(IanaLinkRelations.NEXT).getHref()).startsWith(baseUri.toUriString());
	}

	@Test
	public void usesCustomLinkProvided() {

		Link link = new Link("http://foo:9090", "rel");

		PagedModel<EntityModel<Person>> resources = assembler.toModel(createPage(1), link);

		assertThat(resources.getRequiredLink(IanaLinkRelations.PREV).getHref()).startsWith(link.getHref());
		assertThat(resources.getRequiredLink(IanaLinkRelations.SELF)).isEqualTo(link.withSelfRel());
		assertThat(resources.getRequiredLink(IanaLinkRelations.NEXT).getHref()).startsWith(link.getHref());
	}

	@Test // DATACMNS-358
	public void createsPagedResourcesForOneIndexedArgumentResolver() {

		resolver.setOneIndexedParameters(true);

		AbstractPageRequest request = PageRequest.of(0, 1);
		Page<Person> page = new PageImpl<>(Collections.emptyList(), request, 0);

		assembler.toModel(page);
	}

	@Test // DATACMNS-418, DATACMNS-515
	public void createsACanonicalLinkWithoutTemplateParameters() {

		PagedModel<EntityModel<Person>> resources = assembler.toModel(createPage(1));

		assertThat(resources.getRequiredLink(IanaLinkRelations.SELF).getHref()).doesNotContain("{").doesNotContain("}");
	}

	@Test // DATACMNS-418
	public void invokesCustomElementResourceAssembler() {

		PersonResourceAssembler personAssembler = new PersonResourceAssembler();

		PagedModel<PersonResource> resources = assembler.toModel(createPage(0), personAssembler);

		assertThat(resources.hasLink(IanaLinkRelations.SELF)).isTrue();
		assertThat(resources.hasLink(IanaLinkRelations.NEXT)).isTrue();
		Collection<PersonResource> content = resources.getContent();
		assertThat(content).hasSize(1);
		assertThat(content.iterator().next().name).isEqualTo("Dave");
	}

	@Test // DATAMCNS-563
	public void createsPaginationLinksForOneIndexedArgumentResolverCorrectly() {

		HateoasPageableHandlerMethodArgumentResolver argumentResolver = new HateoasPageableHandlerMethodArgumentResolver();
		argumentResolver.setOneIndexedParameters(true);

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<>(argumentResolver, null);
		PagedModel<EntityModel<Person>> resource = assembler.toModel(createPage(1));

		assertThat(resource.hasLink("prev")).isTrue();
		assertThat(resource.hasLink("next")).isTrue();

		// We expect 2 as the created page has index 1. Pages itself are always 0 indexed, so we created page 2 above.
		assertThat(resource.getMetadata().getNumber()).isEqualTo(2);

		assertThat(getQueryParameters(resource.getRequiredLink("prev"))).containsEntry("page", "1");
		assertThat(getQueryParameters(resource.getRequiredLink("next"))).containsEntry("page", "3");
	}

	@Test // DATACMNS-515
	public void generatedLinksShouldNotBeTemplated() {

		PagedModel<EntityModel<Person>> resources = assembler.toModel(createPage(1));

		assertThat(resources.getRequiredLink(IanaLinkRelations.SELF).getHref()).doesNotContain("{").doesNotContain("}");
		assertThat(resources.getRequiredLink(IanaLinkRelations.NEXT).getHref()).endsWith("?page=2&size=1");
		assertThat(resources.getRequiredLink(IanaLinkRelations.PREV).getHref()).endsWith("?page=0&size=1");
	}

	@Test // DATACMNS-699
	public void generatesEmptyPagedResourceWithEmbeddedWrapper() {

		PagedModel<?> result = assembler.toEmptyModel(EMPTY_PAGE, Person.class);

		Collection<?> content = result.getContent();
		assertThat(content).hasSize(1);

		Object element = content.iterator().next();
		assertThat(element).isInstanceOf(EmbeddedWrapper.class);
		assertThat(((EmbeddedWrapper) element).getRelTargetType()).isEqualTo(Person.class);
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-699
	public void emptyPageCreatorRejectsPageWithContent() {
		assembler.toEmptyModel(createPage(1), Person.class);
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-699
	public void emptyPageCreatorRejectsNullType() {
		assembler.toEmptyModel(EMPTY_PAGE, null);
	}

	@Test // DATACMNS-701
	public void addsFirstAndLastLinksForMultiplePages() {

		PagedModel<EntityModel<Person>> resources = assembler.toModel(createPage(1));

		assertThat(resources.getRequiredLink(IanaLinkRelations.FIRST).getHref()).endsWith("?page=0&size=1");
		assertThat(resources.getRequiredLink(IanaLinkRelations.LAST).getHref()).endsWith("?page=2&size=1");
	}

	@Test // DATACMNS-701
	public void addsFirstAndLastLinksForFirstPage() {

		PagedModel<EntityModel<Person>> resources = assembler.toModel(createPage(0));

		assertThat(resources.getRequiredLink(IanaLinkRelations.FIRST).getHref()).endsWith("?page=0&size=1");
		assertThat(resources.getRequiredLink(IanaLinkRelations.LAST).getHref()).endsWith("?page=2&size=1");
	}

	@Test // DATACMNS-701
	public void addsFirstAndLastLinksForLastPage() {

		PagedModel<EntityModel<Person>> resources = assembler.toModel(createPage(2));

		assertThat(resources.getRequiredLink(IanaLinkRelations.FIRST).getHref()).endsWith("?page=0&size=1");
		assertThat(resources.getRequiredLink(IanaLinkRelations.LAST).getHref()).endsWith("?page=2&size=1");
	}

	@Test // DATACMNS-701
	public void alwaysAddsFirstAndLastLinkIfConfiguredTo() {

		PagedResourcesAssembler<Person> assembler = new PagedResourcesAssembler<>(resolver, null);
		assembler.setForceFirstAndLastRels(true);

		PagedModel<EntityModel<Person>> resources = assembler.toModel(EMPTY_PAGE);

		assertThat(resources.getRequiredLink(IanaLinkRelations.FIRST).getHref()).endsWith("?page=0&size=20");
		assertThat(resources.getRequiredLink(IanaLinkRelations.LAST).getHref()).endsWith("?page=0&size=20");
	}

	@Test // DATACMNS-802
	public void usesCustomPagedResources() {

		RepresentationModelAssembler<Page<Person>, PagedModel<EntityModel<Person>>> assembler = new CustomPagedResourcesAssembler<>(
				resolver, null);

		assertThat(assembler.toModel(EMPTY_PAGE)).isInstanceOf(CustomPagedResources.class);
	}

	@Test // DATACMNS-1042
	public void selfLinkContainsCoordinatesForCurrentPage() {

		PagedModel<EntityModel<Person>> resource = assembler.toModel(createPage(0));

		assertThat(resource.getRequiredLink(IanaLinkRelations.SELF).getHref()).endsWith("?page=0&size=1");
	}

	private static Page<Person> createPage(int index) {

		Pageable request = PageRequest.of(index, 1);

		Person person = new Person();
		person.name = "Dave";

		return new PageImpl<>(Collections.singletonList(person), request, 3);
	}

	private static Map<String, String> getQueryParameters(Link link) {

		UriComponents uriComponents = UriComponentsBuilder.fromUri(URI.create(link.expand().getHref())).build();
		return uriComponents.getQueryParams().toSingleValueMap();
	}

	static class Person {
		String name;
	}

	static class PersonResource extends RepresentationModel<PersonResource> {
		String name;
	}

	static class PersonResourceAssembler implements RepresentationModelAssembler<Person, PersonResource> {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.server.RepresentationModelAssembler#toModel(java.lang.Object)
		 */
		@Override
		public PersonResource toModel(Person entity) {
			PersonResource resource = new PersonResource();
			resource.name = entity.name;
			return resource;
		}
	}

	static class CustomPagedResourcesAssembler<T> extends PagedResourcesAssembler<T> {

		public CustomPagedResourcesAssembler(HateoasPageableHandlerMethodArgumentResolver resolver, UriComponents baseUri) {
			super(resolver, baseUri);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.web.PagedResourcesAssembler#createPagedModel(java.util.List, org.springframework.hateoas.PagedModel.PageMetadata, org.springframework.data.domain.Page)
		 */
		@Override
		protected <R extends RepresentationModel<?>, S> PagedModel<R> createPagedModel(List<R> resources,
				PageMetadata metadata, Page<S> page) {
			return new CustomPagedResources<>(resources, metadata);
		}
	}

	static class CustomPagedResources<R extends RepresentationModel> extends PagedModel<R> {

		public CustomPagedResources(Collection<R> content, PageMetadata metadata) {
			super(content, metadata);
		}
	}
}
