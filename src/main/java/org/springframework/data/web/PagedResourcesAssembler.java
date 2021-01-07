/*
 * Copyright 2013-2021 the original author or authors.
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.RepresentationModel;
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
 * {@link ResourceAssembler} to easily convert {@link Page} instances into {@link PagedResources}.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Nick Williams
 * @author Marcel Overdijk
 */
public class PagedResourcesAssembler<T> implements RepresentationModelAssembler<Page<T>, PagedModel<EntityModel<T>>> {

	private final HateoasPageableHandlerMethodArgumentResolver pageableResolver;
	private final Optional<UriComponents> baseUri;
	private final EmbeddedWrappers wrappers = new EmbeddedWrappers(false);

	private boolean forceFirstAndLastRels = false;

	/**
	 * Creates a new {@link PagedResourcesAssembler} using the given {@link PageableHandlerMethodArgumentResolver} and
	 * base URI. If the former is {@literal null}, a default one will be created. If the latter is {@literal null}, calls
	 * to {@link #toModel(Page)} will use the current request's URI to build the relevant previous and next links.
	 *
	 * @param resolver can be {@literal null}.
	 * @param baseUri can be {@literal null}.
	 */
	public PagedResourcesAssembler(@Nullable HateoasPageableHandlerMethodArgumentResolver resolver,
			@Nullable UriComponents baseUri) {

		this.pageableResolver = resolver == null ? new HateoasPageableHandlerMethodArgumentResolver() : resolver;
		this.baseUri = Optional.ofNullable(baseUri);
	}

	/**
	 * Configures whether to always add {@code first} and {@code last} links to the {@link PagedResources} created.
	 * Defaults to {@literal false} which means that {@code first} and {@code last} links only appear in conjunction with
	 * {@code prev} and {@code next} links.
	 *
	 * @param forceFirstAndLastRels whether to always add {@code first} and {@code last} links to the
	 *          {@link PagedResources} created.
	 * @since 1.11
	 */
	public void setForceFirstAndLastRels(boolean forceFirstAndLastRels) {
		this.forceFirstAndLastRels = forceFirstAndLastRels;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.server.RepresentationModelAssembler#toModel(java.lang.Object)
	 */
	@Override
	public PagedModel<EntityModel<T>> toModel(Page<T> entity) {
		return toModel(entity, EntityModel::of);
	}

	/**
	 * Creates a new {@link PagedResources} by converting the given {@link Page} into a {@link PageMetadata} instance and
	 * wrapping the contained elements into {@link Resource} instances. Will add pagination links based on the given the
	 * self link.
	 *
	 * @param page must not be {@literal null}.
	 * @param selfLink must not be {@literal null}.
	 * @return
	 */
	public PagedModel<EntityModel<T>> toModel(Page<T> page, Link selfLink) {
		return toModel(page, EntityModel::of, selfLink);
	}

	/**
	 * Creates a new {@link PagedResources} by converting the given {@link Page} into a {@link PageMetadata} instance and
	 * using the given {@link ResourceAssembler} to turn elements of the {@link Page} into resources.
	 *
	 * @param page must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @return
	 */
	public <R extends RepresentationModel<?>> PagedModel<R> toModel(Page<T> page,
			RepresentationModelAssembler<T, R> assembler) {
		return createModel(page, assembler, Optional.empty());
	}

	/**
	 * Creates a new {@link PagedResources} by converting the given {@link Page} into a {@link PageMetadata} instance and
	 * using the given {@link ResourceAssembler} to turn elements of the {@link Page} into resources. Will add pagination
	 * links based on the given the self link.
	 *
	 * @param page must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @param link must not be {@literal null}.
	 * @return
	 */
	public <R extends RepresentationModel<?>> PagedModel<R> toModel(Page<T> page,
			RepresentationModelAssembler<T, R> assembler, Link link) {

		Assert.notNull(link, "Link must not be null!");

		return createModel(page, assembler, Optional.of(link));
	}

	/**
	 * Creates a {@link PagedResources} with an empt collection {@link EmbeddedWrapper} for the given domain type.
	 *
	 * @param page must not be {@literal null}, content must be empty.
	 * @param type must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	public PagedModel<?> toEmptyModel(Page<?> page, Class<?> type) {
		return toEmptyModel(page, type, Optional.empty());
	}

	/**
	 * Creates a {@link PagedResources} with an empt collection {@link EmbeddedWrapper} for the given domain type.
	 *
	 * @param page must not be {@literal null}, content must be empty.
	 * @param type must not be {@literal null}.
	 * @param link must not be {@literal null}.
	 * @return
	 * @since 1.11
	 */
	public PagedModel<?> toEmptyModel(Page<?> page, Class<?> type, Link link) {
		return toEmptyModel(page, type, Optional.of(link));
	}

	private PagedModel<?> toEmptyModel(Page<?> page, Class<?> type, Optional<Link> link) {

		Assert.notNull(page, "Page must not be null!");
		Assert.isTrue(!page.hasContent(), "Page must not have any content!");
		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(link, "Link must not be null!");

		PageMetadata metadata = asPageMetadata(page);

		EmbeddedWrapper wrapper = wrappers.emptyCollectionOf(type);
		List<EmbeddedWrapper> embedded = Collections.singletonList(wrapper);

		return addPaginationLinks(PagedModel.of(embedded, metadata), page, link);
	}

	/**
	 * Creates the {@link PagedResources} to be equipped with pagination links downstream.
	 *
	 * @param resources the original page's elements mapped into {@link ResourceSupport} instances.
	 * @param metadata the calculated {@link PageMetadata}, must not be {@literal null}.
	 * @param page the original page handed to the assembler, must not be {@literal null}.
	 * @return must not be {@literal null}.
	 */
	protected <R extends RepresentationModel<?>, S> PagedModel<R> createPagedModel(List<R> resources,
			PageMetadata metadata, Page<S> page) {

		Assert.notNull(resources, "Content resources must not be null!");
		Assert.notNull(metadata, "PageMetadata must not be null!");
		Assert.notNull(page, "Page must not be null!");

		return PagedModel.of(resources, metadata);
	}

	private <S, R extends RepresentationModel<?>> PagedModel<R> createModel(Page<S> page,
			RepresentationModelAssembler<S, R> assembler, Optional<Link> link) {

		Assert.notNull(page, "Page must not be null!");
		Assert.notNull(assembler, "ResourceAssembler must not be null!");

		List<R> resources = new ArrayList<>(page.getNumberOfElements());

		for (S element : page) {
			resources.add(assembler.toModel(element));
		}

		PagedModel<R> resource = createPagedModel(resources, asPageMetadata(page), page);

		return addPaginationLinks(resource, page, link);
	}

	private <R> PagedModel<R> addPaginationLinks(PagedModel<R> resources, Page<?> page, Optional<Link> link) {

		UriTemplate base = getUriTemplate(link);

		boolean isNavigable = page.hasPrevious() || page.hasNext();

		if (isNavigable || forceFirstAndLastRels) {
			resources.add(createLink(base, PageRequest.of(0, page.getSize(), page.getSort()), IanaLinkRelations.FIRST));
		}

		if (page.hasPrevious()) {
			resources.add(createLink(base, page.previousPageable(), IanaLinkRelations.PREV));
		}

		Link selfLink = link.map(Link::withSelfRel)//
				.orElseGet(() -> createLink(base, page.getPageable(), IanaLinkRelations.SELF));

		resources.add(selfLink);

		if (page.hasNext()) {
			resources.add(createLink(base, page.nextPageable(), IanaLinkRelations.NEXT));
		}

		if (isNavigable || forceFirstAndLastRels) {

			int lastIndex = page.getTotalPages() == 0 ? 0 : page.getTotalPages() - 1;

			resources
					.add(createLink(base, PageRequest.of(lastIndex, page.getSize(), page.getSort()), IanaLinkRelations.LAST));
		}

		return resources;
	}

	/**
	 * Returns a default URI string either from the one configured on assembler creatino or by looking it up from the
	 * current request.
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
		pageableResolver.enhance(builder, getMethodParameter(), pageable);

		return Link.of(UriTemplate.of(builder.build().toString()), relation);
	}

	/**
	 * Return the {@link MethodParameter} to be used to potentially qualify the paging and sorting request parameters to.
	 * Default implementations returns {@literal null}, which means the parameters will not be qualified.
	 *
	 * @return
	 * @since 1.7
	 */
	@Nullable
	protected MethodParameter getMethodParameter() {
		return null;
	}

	/**
	 * Creates a new {@link PageMetadata} instance from the given {@link Page}.
	 *
	 * @param page must not be {@literal null}.
	 * @return
	 */
	private PageMetadata asPageMetadata(Page<?> page) {

		Assert.notNull(page, "Page must not be null!");

		int number = pageableResolver.isOneIndexedParameters() ? page.getNumber() + 1 : page.getNumber();

		return new PageMetadata(page.getSize(), number, page.getTotalElements(), page.getTotalPages());
	}

	private String baseUriOrCurrentRequest() {
		return baseUri.map(Object::toString).orElseGet(PagedResourcesAssembler::currentRequest);
	}

	private static String currentRequest() {
		return ServletUriComponentsBuilder.fromCurrentRequest().build().toString();
	}
}
