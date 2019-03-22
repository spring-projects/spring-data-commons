/*
 * Copyright 2013-2014 the original author or authors.
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
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
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

	/**
	 * Adds the pagination parameters for all parameters not already present in the given {@link Link}.
	 * 
	 * @param link must not be {@literal null}.
	 * @return
	 */
	public Link appendPaginationParameterTemplates(Link link) {

		Assert.notNull(link, "Link must not be null!");
		return createLink(new UriTemplate(link.getHref()), null, link.getRel());
	}

	private <S, R extends ResourceSupport> PagedResources<R> createResource(Page<S> page,
			ResourceAssembler<S, R> assembler, Link link) {

		Assert.notNull(page, "Page must not be null!");
		Assert.notNull(assembler, "ResourceAssembler must not be null!");

		List<R> resources = new ArrayList<R>(page.getNumberOfElements());

		for (S element : page) {
			resources.add(assembler.toResource(element));
		}

		UriTemplate base = new UriTemplate(link == null ? getDefaultUriString() : link.getHref());

		PagedResources<R> pagedResources = new PagedResources<R>(resources, asPageMetadata(page));
		pagedResources.add(createLink(base, null, Link.REL_SELF));

		if (page.hasNext()) {
			pagedResources.add(createLink(base, page.nextPageable(), Link.REL_NEXT));
		}

		if (page.hasPrevious()) {
			pagedResources.add(createLink(base, page.previousPageable(), Link.REL_PREVIOUS));
		}

		return pagedResources;
	}

	/**
	 * Returns a default URI string either from the one configured on assembler creatino or by looking it up from the
	 * current request.
	 * 
	 * @return
	 */
	private String getDefaultUriString() {
		return baseUri == null ? ServletUriComponentsBuilder.fromCurrentRequest().build().toString() : baseUri.toString();
	}

	/**
	 * Creates a {@link Link} with the given rel that will be based on the given {@link UriTemplate} but enriched with the
	 * values of the given {@link Pageable} (if not {@literal null}) and the missing parameters added as template
	 * variables.
	 * 
	 * @param base must not be {@literal null}.
	 * @param pageable can be {@literal null}
	 * @param rel must not be {@literal null} or empty.
	 * @return
	 */
	private Link createLink(UriTemplate base, Pageable pageable, String rel) {

		UriComponentsBuilder builder = fromUri(base.expand());
		pageableResolver.enhance(builder, getMethodParameter(), pageable);

		UriComponents components = builder.build();
		TemplateVariables variables = new TemplateVariables(base.getVariables());
		variables = variables.concat(pageableResolver.getPaginationTemplateVariables(getMethodParameter(), components));

		return new Link(new UriTemplate(components.toString()).with(variables), rel);
	}

	/**
	 * Return the {@link MethodParameter} to be used to potentially qualify the paging and sorting request parameters to.
	 * Default implementations returns {@literal null}, which means the parameters will not be qualified.
	 * 
	 * @return
	 * @since 1.7
	 */
	protected MethodParameter getMethodParameter() {
		return null;
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
