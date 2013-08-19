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

import static org.springframework.web.util.UriComponentsBuilder.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.ResourceSupport;
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
 */
public class PagedResourcesAssembler<T> implements ResourceAssembler<Page<T>, PagedResources<Resource<T>>> {

	private final HateoasPageableHandlerMethodArgumentResolver pageableResolver;
	private final UriComponents baseUri;

	/**
	 * Creates a new {@link PagedResourcesAssembler} using the given {@link PageableHandlerMethodArgumentResolver} and
	 * base URI. If the former is {@literal null}, a default one will be created. If the latter is {@literal null}, calls
	 * to {@link #toResource(Page)} will use the current request's URI to build the relevant previous and next links.
	 * 
	 * @param resolver
	 * @param baseUri
	 */
	public PagedResourcesAssembler(HateoasPageableHandlerMethodArgumentResolver resolver, UriComponents baseUri) {

		this.pageableResolver = resolver == null ? new HateoasPageableHandlerMethodArgumentResolver() : resolver;
		this.baseUri = baseUri;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.ResourceAssembler#toResource(java.lang.Object)
	 */
	@Override
	public PagedResources<Resource<T>> toResource(Page<T> entity) {
		return toResource(entity, new SimplePagedResourceAssembler<T>());
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
	public PagedResources<Resource<T>> toResource(Page<T> page, Link selfLink) {
		return toResource(page, new SimplePagedResourceAssembler<T>(), selfLink);
	}

	/**
	 * Creates a new {@link PagedResources} by converting the given {@link Page} into a {@link PageMetadata} instance and
	 * using the given {@link ResourceAssembler} to turn elements of the {@link Page} into resources.
	 * 
	 * @param page must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @return
	 */
	public <R extends ResourceSupport> PagedResources<R> toResource(Page<T> page, ResourceAssembler<T, R> assembler) {
		return createResource(page, assembler, null);
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
	public <R extends ResourceSupport> PagedResources<R> toResource(Page<T> page, ResourceAssembler<T, R> assembler,
			Link link) {

		Assert.notNull(link, "Link must not be null!");
		return createResource(page, assembler, link);
	}

	private <S, R extends ResourceSupport> PagedResources<R> createResource(Page<S> page,
			ResourceAssembler<S, R> assembler, Link link) {

		Assert.notNull(page, "Page must not be null!");
		Assert.notNull(assembler, "ResourceAssembler must not be null!");

		List<R> resources = new ArrayList<R>(page.getNumberOfElements());

		for (S element : page) {
			resources.add(assembler.toResource(element));
		}

		PagedResources<R> pagedResources = new PagedResources<R>(resources, asPageMetadata(page));
		return addPaginationLinks(pagedResources, page, link == null ? getDefaultUriString().toUriString() : link.getHref());
	}

	private UriComponents getDefaultUriString() {
		return baseUri == null ? ServletUriComponentsBuilder.fromCurrentRequest().build() : baseUri;
	}

	private <R extends ResourceSupport> PagedResources<R> addPaginationLinks(PagedResources<R> resources, Page<?> page,
			String uri) {

		if (page.hasNextPage()) {
			foo(resources, page.nextPageable(), uri, Link.REL_NEXT);
		}

		if (page.hasPreviousPage()) {
			foo(resources, page.previousPageable(), uri, Link.REL_PREVIOUS);
		}

		return resources;
	}

	private void foo(PagedResources<?> resources, Pageable pageable, String uri, String rel) {

		UriComponentsBuilder builder = fromUriString(uri);
		pageableResolver.enhance(builder, null, pageable);
		resources.add(new Link(builder.build().toUriString(), rel));
	}

	/**
	 * Creates a new {@link PageMetadata} instance from the given {@link Page}.
	 * 
	 * @param page must not be {@literal null}.
	 * @return
	 */
	private static <T> PageMetadata asPageMetadata(Page<T> page) {

		Assert.notNull(page, "Page must not be null!");
		return new PageMetadata(page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages());
	}

	private static class SimplePagedResourceAssembler<T> implements ResourceAssembler<T, Resource<T>> {

		@Override
		public Resource<T> toResource(T entity) {
			return new Resource<T>(entity);
		}
	}
}
