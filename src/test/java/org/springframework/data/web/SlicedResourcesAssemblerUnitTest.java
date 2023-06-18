/*
 * Copyright 2022-2023 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.SlicedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link SlicedResourcesAssembler}.
 *
 * @author Michael Schout
 * @author Oliver Drotbohm
 * @since 3.1
 */
class SlicedResourcesAssemblerUnitTest {

	static final Pageable PAGEABLE = PageRequest.of(0, 20);
	static final Slice<Person> EMPTY_SLICE = new SliceImpl<>(Collections.emptyList(), PAGEABLE, false);

	HateoasPageableHandlerMethodArgumentResolver resolver = new HateoasPageableHandlerMethodArgumentResolver();
	SlicedResourcesAssembler<Person> assembler = new SlicedResourcesAssembler<>(resolver, null);

	@BeforeEach
	void setUp() {
		WebTestUtils.initWebTest();
	}

	@Test // GH-1307
	void addsNextLinkForFirstSlice() {

		var resources = assembler.toModel(createSlice(0));

		assertThat(resources.getLink(IanaLinkRelations.PREV)).isEmpty();
		assertThat(resources.getLink(IanaLinkRelations.SELF)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.NEXT)).isNotEmpty();
	}

	@Test // GH-1307
	void addsPreviousAndNextLinksForMiddleSlice() {

		var resources = assembler.toModel(createSlice(1));

		assertThat(resources.getLink(IanaLinkRelations.PREV)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.SELF)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.NEXT)).isNotEmpty();
	}

	@Test // GH-1307
	void addsPreviousLinkForLastSlice() {

		var resources = assembler.toModel(createSlice(2));

		assertThat(resources.getLink(IanaLinkRelations.PREV)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.SELF)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.NEXT)).isEmpty();
	}

	@Test // GH-1307
	void usesBaseUriIfConfigured() {

		var baseUri = UriComponentsBuilder.fromUriString("https://foo:9090").build();

		var assembler = new SlicedResourcesAssembler<Person>(resolver, baseUri);
		var resources = assembler.toModel(createSlice(1));

		assertThat(resources.getRequiredLink(IanaLinkRelations.PREV).getHref()).startsWith(baseUri.toUriString());
		assertThat(resources.getRequiredLink(IanaLinkRelations.SELF)).isNotNull();
		assertThat(resources.getRequiredLink(IanaLinkRelations.NEXT).getHref()).startsWith(baseUri.toUriString());
	}

	@Test // GH-1307
	void usesCustomLinkProvided() {

		var link = Link.of("https://foo:9090", "rel");

		var resources = assembler.toModel(createSlice(1), link);

		assertThat(resources.getRequiredLink(IanaLinkRelations.PREV).getHref()).startsWith(link.getHref());
		assertThat(resources.getRequiredLink(IanaLinkRelations.SELF)).isEqualTo(link.withSelfRel());
		assertThat(resources.getRequiredLink(IanaLinkRelations.NEXT).getHref()).startsWith(link.getHref());
	}

	@Test // GH-1307
	void createsSlicedResourcesForOneIndexedArgumentResolver() {

		resolver.setOneIndexedParameters(true);

		AbstractPageRequest request = PageRequest.of(0, 1);
		Slice<Person> slice = new SliceImpl<>(Collections.emptyList(), request, true);

		assembler.toModel(slice);
	}

	@Test // GH-1307
	void createsACanonicalLinkWithoutTemplateParameters() {

		var resources = assembler.toModel(createSlice(1));

		assertThat(resources.getRequiredLink(IanaLinkRelations.SELF).getHref()).doesNotContain("{").doesNotContain("}");
	}

	@Test // GH-1307
	void invokesCustomElementResourceAssembler() {

		var personAssembler = new PersonResourceAssembler();

		var resources = assembler.toModel(createSlice(0), personAssembler);

		assertThat(resources.hasLink(IanaLinkRelations.SELF)).isTrue();
		assertThat(resources.hasLink(IanaLinkRelations.NEXT)).isTrue();

		var content = resources.getContent();
		assertThat(content).hasSize(1);
		assertThat(content.iterator().next().name).isEqualTo("Dave");
	}

	@Test // GH-1307
	void createsPaginationLinksForOneIndexedArgumentResolverCorrectly() {

		var argumentResolver = new HateoasPageableHandlerMethodArgumentResolver();
		argumentResolver.setOneIndexedParameters(true);

		var assembler = new SlicedResourcesAssembler<Person>(argumentResolver, null);
		var resource = assembler.toModel(createSlice(1));

		assertThat(resource.hasLink("prev")).isTrue();
		assertThat(resource.hasLink("next")).isTrue();

		// We expect 2 as the created slice has index 1. slices are always 0 indexed, so we
		// created page 2 above.
		assertThat(resource.getMetadata().getNumber()).isEqualTo(2);

		assertThat(getQueryParameters(resource.getRequiredLink("prev"))).containsEntry("page", "1");
		assertThat(getQueryParameters(resource.getRequiredLink("next"))).containsEntry("page", "3");
	}

	@Test // GH-1307
	void generatedLinksShouldNotBeTemplated() {

		var resources = assembler.toModel(createSlice(1));

		assertThat(resources.getRequiredLink(IanaLinkRelations.SELF).getHref()).doesNotContain("{").doesNotContain("}");
		assertThat(resources.getRequiredLink(IanaLinkRelations.NEXT).getHref()).endsWith("?page=2&size=1");
		assertThat(resources.getRequiredLink(IanaLinkRelations.PREV).getHref()).endsWith("?page=0&size=1");
	}

	@Test // GH-1307
	void generatesEmptySliceResourceWithEmbeddedWrapper() {

		var result = assembler.toEmptyModel(EMPTY_SLICE, Person.class);

		var content = result.getContent();
		assertThat(content).hasSize(1);

		var element = content.iterator().next();
		assertThat(element).isInstanceOf(EmbeddedWrapper.class);
		assertThat(((EmbeddedWrapper) element).getRelTargetType()).isEqualTo(Person.class);
	}

	@Test // GH-1307
	void emptySliceCreatorRejectsSliceWithContent() {
		assertThatIllegalArgumentException().isThrownBy(() -> assembler.toEmptyModel(createSlice(1), Person.class));
	}

	@Test // GH-1307
	void emptySliceCreatorRejectsNullType() {
		assertThatIllegalArgumentException().isThrownBy(() -> assembler.toEmptyModel(EMPTY_SLICE, null));
	}

	@Test // GH-1307
	void addsFirstLinkForMultipleSlices() {
		var resources = assembler.toModel(createSlice(1));

		assertThat(resources.getRequiredLink(IanaLinkRelations.FIRST).getHref()).endsWith("?page=0&size=1");
	}

	@Test // GH-1307
	void addsFirstLinkForFirstSlice() {
		var resources = assembler.toModel(createSlice(0));

		assertThat(resources.getRequiredLink(IanaLinkRelations.FIRST).getHref()).endsWith("?page=0&size=1");
	}

	@Test // GH-1307
	void addsFirstLinkForLastSlice() {
		var resources = assembler.toModel(createSlice(2));

		assertThat(resources.getRequiredLink(IanaLinkRelations.FIRST).getHref()).endsWith("?page=0&size=1");
	}

	@Test // GH-1307
	void alwaysAddsFirstLinkIfConfiguredTo() {

		var assembler = new SlicedResourcesAssembler<Person>(resolver, null);
		assembler.setForceFirstRel(true);

		var resources = assembler.toModel(EMPTY_SLICE);

		assertThat(resources.getRequiredLink(IanaLinkRelations.FIRST).getHref()).endsWith("?page=0&size=20");
	}

	@Test // GH-1307
	void usesCustomSlicedResources() {

		var assembler = new CustomSlicedResourcesAssembler<Person>(resolver, null);

		assertThat(assembler.toModel(EMPTY_SLICE)).isInstanceOf(CustomSlicedResources.class);
	}

	@Test // GH-1307
	void selfLinkContainsCoordinatesForCurrentSlice() {
		var resource = assembler.toModel(createSlice(0));

		assertThat(resource.getRequiredLink(IanaLinkRelations.SELF).getHref()).endsWith("?page=0&size=1");
	}

	@Test // GH-1307
	void keepsRequestParametersOfOriginalRequestUri() {
		WebTestUtils.initWebTest(new MockHttpServletRequest("GET", "/sample?foo=bar"));

		var model = assembler.toModel(createSlice(1));

		assertThat(model.getRequiredLink(IanaLinkRelations.FIRST).getHref())
				.isEqualTo("http://localhost/sample?foo=bar&page=0&size=1");
	}

	private static Slice<Person> createSlice(int index) {

		Pageable request = PageRequest.of(index, 1);

		var person = new Person();
		person.name = "Dave";

		boolean hasNext = index < 2;

		return new SliceImpl<>(Collections.singletonList(person), request, hasNext);
	}

	private static Map<String, String> getQueryParameters(Link link) {
		var uriComponents = UriComponentsBuilder.fromUri(URI.create(link.expand().getHref())).build();
		return uriComponents.getQueryParams().toSingleValueMap();
	}

	static class Person {
		String name;
	}

	static class PersonResource extends RepresentationModel<PersonResource> {
		String name;
	}

	static class PersonResourceAssembler implements RepresentationModelAssembler<Person, PersonResource> {
		@Override
		public PersonResource toModel(Person entity) {
			var resource = new PersonResource();
			resource.name = entity.name;
			return resource;
		}
	}

	static class CustomSlicedResourcesAssembler<T> extends SlicedResourcesAssembler<T> {
		CustomSlicedResourcesAssembler(HateoasPageableHandlerMethodArgumentResolver resolver, UriComponents baseUri) {
			super(resolver, baseUri);
		}

		@Override
		protected <R extends RepresentationModel<?>, S> SlicedModel<R> createSlicedModel(List<R> resources,
				SlicedModel.SliceMetadata metadata, Slice<S> slice) {
			return new CustomSlicedResources<>(resources, metadata);
		}
	}

	static class CustomSlicedResources<R extends RepresentationModel> extends SlicedModel<R> {
		CustomSlicedResources(Collection<R> content, SliceMetadata metadata) {
			super(content, metadata);
		}
	}
}
