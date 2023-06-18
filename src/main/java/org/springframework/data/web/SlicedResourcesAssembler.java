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

import static org.springframework.web.util.UriComponentsBuilder.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.SlicedModel;
import org.springframework.hateoas.SlicedModel.SliceMetadata;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link RepresentationModelAssembler} to easily convert {@link Slice} instances into {@link SlicedModel}.
 *
 * @author Michael Schout
 * @author Oliver Drotbohm
 * @since 3.1
 */
public class SlicedResourcesAssembler<T>
		implements RepresentationModelAssembler<Slice<T>, SlicedModel<EntityModel<T>>> {

	private final HateoasPageableHandlerMethodArgumentResolver pageableResolver;
	private final Optional<UriComponents> baseUri;
	private final EmbeddedWrappers wrappers = new EmbeddedWrappers(false);

	private boolean forceFirstRel = false;
	private @Nullable MethodParameter parameter;

	/**
	 * Creates a new {@link SlicedResourcesAssembler} using the given {@link PageableHandlerMethodArgumentResolver} and
	 * base URI. If the former is {@literal null}, a default one will be created. If the latter is {@literal null}, calls
	 * to {@link #toModel(Slice)} will use the current request's URI to build the relevant previous and next links.
	 *
	 * @param resolver can be {@literal null}.
	 * @param baseUri can be {@literal null}.
	 */
	public SlicedResourcesAssembler(@Nullable HateoasPageableHandlerMethodArgumentResolver resolver,
			@Nullable UriComponents baseUri) {
		this(resolver, Optional.ofNullable(baseUri), null);
	}

	private SlicedResourcesAssembler(@Nullable HateoasPageableHandlerMethodArgumentResolver resolver,
			@Nullable Optional<UriComponents> baseUri, @Nullable MethodParameter parameter) {

		this.pageableResolver = resolver == null ? new HateoasPageableHandlerMethodArgumentResolver() : resolver;
		this.baseUri = baseUri;
		this.parameter = parameter;
	}

	/**
	 * Configures whether to always add {@code first} links to the {@link SlicedModel} * created. Defaults to
	 * {@literal false} which means that {@code first} links onlys appear in conjunction with {@code prev} and
	 * {@code next} links.
	 *
	 * @param forceFirstRel whether to always add {@code first} links to the {@link SlicedModel} created.
	 */
	public void setForceFirstRel(boolean forceFirstRel) {
		this.forceFirstRel = forceFirstRel;
	}

	/**
	 * Creates a new {@link SlicedResourcesAssembler} with the given reference {@link MethodParameter}.
	 *
	 * @param parameter can be {@literal null}.
	 * @return will never be {@literal null}.
	 * @since 3.1
	 */
	public SlicedResourcesAssembler<T> withParameter(@Nullable MethodParameter parameter) {
		return new SlicedResourcesAssembler<>(pageableResolver, baseUri, parameter);
	}

	@Override
	public SlicedModel<EntityModel<T>> toModel(Slice<T> entity) {
		return toModel(entity, EntityModel::of);
	}

	/**
	 * Creates a new {@link SlicedModel} by converting the given {@link Slice} into a {@link SliceMetadata} instance and
	 * wrapping the contained elements into * {@link SlicedModel} instances. Will add pagination links based on the given
	 * self link.
	 *
	 * @param slice must not be {@literal null}.
	 * @param selfLink must not be {@literal null}.
	 * @return
	 */
	public SlicedModel<EntityModel<T>> toModel(Slice<T> slice, Link selfLink) {
		return toModel(slice, EntityModel::of, selfLink);
	}

	/**
	 * Creates a new {@link SlicedModel} by converting the given {@link Slice} into a {@link SliceMetadata} instance and
	 * using the given {@link SlicedModel} to turn elements of the {@link Slice} into resources.
	 *
	 * @param slice must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @return
	 */
	public <R extends RepresentationModel<?>> SlicedModel<R> toModel(Slice<T> slice,
			RepresentationModelAssembler<T, R> assembler) {
		return createModel(slice, assembler, Optional.empty());
	}

	/**
	 * Creates a new {@link SlicedModel} by converting the given {@link Slice} into a {@link SliceMetadata} instance and
	 * using the given {@link SlicedModel} to turn elements of the {@link Slice} into resources. Will add pagination links
	 * based on the given the self link.
	 *
	 * @param slice must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @param link must not be {@literal null}.
	 * @return
	 */
	public <R extends RepresentationModel<?>> SlicedModel<R> toModel(Slice<T> slice,
			RepresentationModelAssembler<T, R> assembler, Link link) {
		return createModel(slice, assembler, Optional.of(link));
	}

	/**
	 * Creates a {@link SlicedModel} with an empty collection {@link EmbeddedWrapper} for the given domain type.
	 *
	 * @param slice must not be {@literal null}, content must be empty.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public SlicedModel<?> toEmptyModel(Slice<?> slice, Class<?> type) {
		return toEmptyModel(slice, type, Optional.empty());
	}

	/**
	 * Creates a {@link SlicedModel} with an empty collection {@link EmbeddedWrapper} for the given domain type.
	 *
	 * @param slice must not be {@literal null}, content must be empty.
	 * @param type must not be {@literal null}.
	 * @param link must not be {@literal null}.
	 * @return
	 */
	public SlicedModel<?> toEmptyModel(Slice<?> slice, Class<?> type, Link link) {
		return toEmptyModel(slice, type, Optional.of(link));
	}

	public SlicedModel<?> toEmptyModel(Slice<?> slice, Class<?> type, Optional<Link> link) {

		Assert.notNull(slice, "Slice must not be null");
		Assert.isTrue(!slice.hasContent(), "Slice must not have any content");
		Assert.notNull(type, "Type must not be null");
		Assert.notNull(link, "Link must not be null");

		SliceMetadata metadata = asSliceMetadata(slice);

		EmbeddedWrapper wrapper = wrappers.emptyCollectionOf(type);
		List<EmbeddedWrapper> embedded = Collections.singletonList(wrapper);

		return addPaginationLinks(SlicedModel.of(embedded, metadata), slice, link);
	}

	/**
	 * Creates the {@link SlicedModel} to be equipped with pagination links downstream.
	 *
	 * @param resources the original slices's elements mapped into {@link RepresentationModel} instances.
	 * @param metadata the calculated {@link SliceMetadata}, must not be {@literal null}.
	 * @param slice the original page handed to the assembler, must not be {@literal null}.
	 * @return must not be {@literal null}.
	 */
	protected <R extends RepresentationModel<?>, S> SlicedModel<R> createSlicedModel(List<R> resources,
			SliceMetadata metadata, Slice<S> slice) {
		Assert.notNull(resources, "Content resources must not be null");
		Assert.notNull(metadata, "SliceMetadata must not be null");
		Assert.notNull(slice, "Slice must not be null");

		return SlicedModel.of(resources, metadata);
	}

	private <S, R extends RepresentationModel<?>> SlicedModel<R> createModel(Slice<S> slice,
			RepresentationModelAssembler<S, R> assembler, Optional<Link> link) {
		Assert.notNull(slice, "Slice must not be null");
		Assert.notNull(assembler, "ResourceAssembler must not be null");

		List<R> resources = new ArrayList<>(slice.getNumberOfElements());

		for (S element : slice) {
			resources.add(assembler.toModel(element));
		}

		SlicedModel<R> resource = createSlicedModel(resources, asSliceMetadata(slice), slice);

		return addPaginationLinks(resource, slice, link);
	}

	private <R> SlicedModel<R> addPaginationLinks(SlicedModel<R> resources, Slice<?> slice, Optional<Link> link) {
		UriTemplate base = getUriTemplate(link);

		boolean isNavigable = slice.hasPrevious() || slice.hasNext();

		if (isNavigable || forceFirstRel) {
			resources.add(
					createLink(base, PageRequest.of(0, slice.getSize(), slice.getSort()), IanaLinkRelations.FIRST));
		}

		Link selfLink = link.map(Link::withSelfRel)
				.orElseGet(() -> createLink(base, slice.getPageable(), IanaLinkRelations.SELF));

		resources.add(selfLink);

		if (slice.hasPrevious()) {
			resources.add(createLink(base, slice.previousPageable(), IanaLinkRelations.PREV));
		}

		if (slice.hasNext()) {
			resources.add(createLink(base, slice.nextPageable(), IanaLinkRelations.NEXT));
		}

		return resources;
	}

	/**
	 * Returns a default URI string either from the one configured on then assembler or by looking it up from the current
	 * request.
	 *
	 * @return
	 */
	private UriTemplate getUriTemplate(Optional<Link> baseLink) {
		return UriTemplate.of(baseLink.map(Link::getHref).orElseGet(this::baseUriOrCurrentRequest));
	}

	/**
	 * Creates a {@link Link} with the given {@link LinkRelation} that will be based on the given {@link UriTemplate} but
	 * enriched with the values of the given {@link Pageable} (if not {@literal null}).
	 *
	 * @param base must not be {@literal null}.
	 * @param pageable can be {@literal null}
	 * @param relation must not be {@literal null}.
	 * @return
	 */
	private Link createLink(UriTemplate base, Pageable pageable, LinkRelation relation) {
		UriComponentsBuilder builder = fromUri(base.expand());
		pageableResolver.enhance(builder, parameter, pageable);

		return Link.of(UriTemplate.of(builder.build().toString()), relation);
	}

	/**
	 * Creates a new {@link SliceMetadata} instance from the given {@link Slice}.
	 *
	 * @param slice must not be {@literal null}.
	 * @return
	 */
	private SliceMetadata asSliceMetadata(Slice<?> slice) {
		Assert.notNull(slice, "Slice must not be null");

		int number = pageableResolver.isOneIndexedParameters() ? slice.getNumber() + 1 : slice.getNumber();

		return new SliceMetadata(slice.getSize(), number);
	}

	private String baseUriOrCurrentRequest() {
		return baseUri.map(Object::toString).orElseGet(SlicedResourcesAssembler::currentRequest);
	}

	private static String currentRequest() {
		return ServletUriComponentsBuilder.fromCurrentRequest().build().toString();
	}
}
