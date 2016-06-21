/*
 * Copyright 2008-2016 the original author or authors.
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
package org.springframework.data.repository.query.parser;

import lombok.Getter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Class to parse a {@link String} into a tree or {@link OrPart}s consisting of simple {@link Part} instances in turn.
 * Takes a domain class as well to validate that each of the {@link Part}s are referring to a property of the domain
 * class. The {@link PartTree} can then be used to build queries based on its API instead of parsing the method name for
 * each query execution.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class PartTree implements Streamable<OrPart> {

	/*
	 * We look for a pattern of: keyword followed by
	 *
	 *  an upper-case letter that has a lower-case variant \p{Lu}
	 * OR
	 *  any other letter NOT in the BASIC_LATIN Uni-code Block \\P{InBASIC_LATIN} (like Chinese, Korean, Japanese, etc.).
	 *
	 * @see http://www.regular-expressions.info/unicode.html
	 * @see http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#ubc
	 */
	private static final String KEYWORD_TEMPLATE = "(%s)(?=(\\p{Lu}|\\P{InBASIC_LATIN}))";
	private static final String QUERY_PATTERN = "find|read|get|query|stream";
	private static final String COUNT_PATTERN = "count";
	private static final String EXISTS_PATTERN = "exists";
	private static final String DELETE_PATTERN = "delete|remove";
	private static final Pattern PREFIX_TEMPLATE = Pattern.compile( //
			"^(" + QUERY_PATTERN + "|" + COUNT_PATTERN + "|" + EXISTS_PATTERN + "|" + DELETE_PATTERN + ")((\\p{Lu}.*?))??By");

	/**
	 * The subject, for example "findDistinctUserByNameOrderByAge" would have the subject "DistinctUser".
	 */
	private final Subject subject;

	/**
	 * The subject, for example "findDistinctUserByNameOrderByAge" would have the predicate "NameOrderByAge".
	 */
	private final Predicate predicate;

	/**
	 * Creates a new {@link PartTree} by parsing the given {@link String}.
	 * 
	 * @param source the {@link String} to parse
	 * @param domainClass the domain class to check individual parts against to ensure they refer to a property of the
	 *          class
	 */
	public PartTree(String source, Class<?> domainClass) {

		Assert.notNull(source, "Source must not be null");
		Assert.notNull(domainClass, "Domain class must not be null");

		Matcher matcher = PREFIX_TEMPLATE.matcher(source);

		if (!matcher.find()) {
			this.subject = new Subject(Optional.empty());
			this.predicate = new Predicate(source, domainClass);
		} else {
			this.subject = new Subject(Optional.of(matcher.group(0)));
			this.predicate = new Predicate(source.substring(matcher.group().length()), domainClass);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<OrPart> iterator() {
		return predicate.iterator();
	}

	/**
	 * Returns the {@link Sort} specification parsed from the source or <tt>null</tt>.
	 * 
	 * @return the sort
	 */
	public Sort getSort() {
		return predicate.getOrderBySource().toSort();
	}

	/**
	 * Returns whether we indicate distinct lookup of entities.
	 * 
	 * @return {@literal true} if distinct
	 */
	public boolean isDistinct() {
		return subject.isDistinct();
	}

	/**
	 * Returns whether a count projection shall be applied.
	 * 
	 * @return
	 */
	public boolean isCountProjection() {
		return subject.isCountProjection();
	}

	/**
	 * Returns whether an exists projection shall be applied.
	 *
	 * @return
	 * @since 1.13
	 */
	public Boolean isExistsProjection() {
		return subject.isExistsProjection();
	}

	/**
	 * return true if the created {@link PartTree} is meant to be used for delete operation.
	 * 
	 * @return
	 * @since 1.8
	 */
	public boolean isDelete() {
		return subject.isDelete();
	}

	/**
	 * Return {@literal true} if the create {@link PartTree} is meant to be used for a query with limited maximal results.
	 * 
	 * @return
	 * @since 1.9
	 */
	public boolean isLimiting() {
		return getMaxResults() != null;
	}

	/**
	 * Return the number of maximal results to return or {@literal null} if not restricted.
	 * 
	 * @return
	 * @since 1.9
	 */
	public Integer getMaxResults() {
		return subject.getMaxResults().orElse(null);
	}

	/**
	 * Returns an {@link Iterable} of all parts contained in the {@link PartTree}.
	 * 
	 * @return the iterable {@link Part}s
	 */
	public Streamable<Part> getParts() {
		return Streamable.of(this.stream().flatMap(OrPart::stream).collect(Collectors.toList()));
	}

	/**
	 * Returns all {@link Part}s of the {@link PartTree} of the given {@link Type}.
	 * 
	 * @param type
	 * @return
	 */
	public Streamable<Part> getParts(Type type) {

		return Streamable.of(getParts().stream()//
				.filter(part -> part.getType().equals(type))//
				.collect(Collectors.toList()));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return String.format("%s %s", StringUtils.collectionToDelimitedString(predicate.nodes, " or "),
				predicate.getOrderBySource().toString()).trim();
	}

	/**
	 * Splits the given text at the given keywords. Expects camel-case style to only match concrete keywords and not
	 * derivatives of it.
	 * 
	 * @param text the text to split
	 * @param keyword the keyword to split around
	 * @return an array of split items
	 */
	private static String[] split(String text, String keyword) {

		Pattern pattern = Pattern.compile(String.format(KEYWORD_TEMPLATE, keyword));
		return pattern.split(text);
	}

	/**
	 * A part of the parsed source that results from splitting up the resource around {@literal Or} keywords. Consists of
	 * {@link Part}s that have to be concatenated by {@literal And}.
	 */
	public static class OrPart implements Streamable<Part> {

		private final List<Part> children;

		/**
		 * Creates a new {@link OrPart}.
		 * 
		 * @param source the source to split up into {@literal And} parts in turn.
		 * @param domainClass the domain class to check the resulting {@link Part}s against.
		 * @param alwaysIgnoreCase if always ignoring case
		 */
		OrPart(String source, Class<?> domainClass, boolean alwaysIgnoreCase) {

			String[] split = split(source, "And");

			this.children = Arrays.stream(split)//
					.filter(part -> StringUtils.hasText(part))//
					.map(part -> new Part(part, domainClass, alwaysIgnoreCase))//
					.collect(Collectors.toList());
		}

		public Iterator<Part> iterator() {
			return children.iterator();
		}

		@Override
		public String toString() {
			return StringUtils.collectionToDelimitedString(children, " and ");
		}
	}

	/**
	 * Represents the subject part of the query. E.g. {@code findDistinctUserByNameOrderByAge} would have the subject
	 * {@code DistinctUser}.
	 * 
	 * @author Phil Webb
	 * @author Oliver Gierke
	 * @author Christoph Strobl
	 * @author Thomas Darimont
	 */
	private static class Subject {

		private static final String DISTINCT = "Distinct";
		private static final Pattern COUNT_BY_TEMPLATE = Pattern.compile("^count(\\p{Lu}.*?)??By");
		private static final Pattern EXISTS_BY_TEMPLATE = Pattern.compile("^(" + EXISTS_PATTERN + ")(\\p{Lu}.*?)??By");
		private static final Pattern DELETE_BY_TEMPLATE = Pattern.compile("^(" + DELETE_PATTERN + ")(\\p{Lu}.*?)??By");
		private static final String LIMITING_QUERY_PATTERN = "(First|Top)(\\d*)?";
		private static final Pattern LIMITED_QUERY_TEMPLATE = Pattern
				.compile("^(" + QUERY_PATTERN + ")(" + DISTINCT + ")?" + LIMITING_QUERY_PATTERN + "(\\p{Lu}.*?)??By");

		private final boolean distinct;
		private final boolean count;
		private final boolean exists;
		private final boolean delete;
		private final Optional<Integer> maxResults;

		public Subject(Optional<String> subject) {

			this.distinct = subject.map(it -> it.contains(DISTINCT)).orElse(false);
			this.count = matches(subject, COUNT_BY_TEMPLATE);
			this.exists = matches(subject, EXISTS_BY_TEMPLATE);
			this.delete = matches(subject, DELETE_BY_TEMPLATE);
			this.maxResults = returnMaxResultsIfFirstKSubjectOrNull(subject);
		}

		/**
		 * @param subject
		 * @return
		 * @since 1.9
		 */
		private Optional<Integer> returnMaxResultsIfFirstKSubjectOrNull(Optional<String> subject) {

			return subject.map(it -> {

				Matcher grp = LIMITED_QUERY_TEMPLATE.matcher(it);

				if (!grp.find()) {
					return null;
				}

				return StringUtils.hasText(grp.group(4)) ? Integer.valueOf(grp.group(4)) : 1;
			});

		}

		/**
		 * Returns {@literal true} if {@link Subject} matches {@link #DELETE_BY_TEMPLATE}.
		 * 
		 * @return
		 * @since 1.8
		 */
		public boolean isDelete() {
			return delete;
		}

		public boolean isCountProjection() {
			return count;
		}

		/**
		 * Returns {@literal true} if {@link Subject} matches {@link #EXISTS_BY_TEMPLATE}.
		 *
		 * @return
		 * @since 1.13
		 */
		public boolean isExistsProjection() {
			return exists;
		}

		public boolean isDistinct() {
			return distinct;
		}

		public Optional<Integer> getMaxResults() {
			return maxResults;
		}

		private final boolean matches(Optional<String> subject, Pattern pattern) {
			return subject.map(it -> pattern.matcher(it).find()).orElse(false);
		}
	}

	/**
	 * Represents the predicate part of the query.
	 * 
	 * @author Oliver Gierke
	 * @author Phil Webb
	 */
	private static class Predicate implements Streamable<OrPart> {

		private static final Pattern ALL_IGNORE_CASE = Pattern.compile("AllIgnor(ing|e)Case");
		private static final String ORDER_BY = "OrderBy";

		private final List<OrPart> nodes;
		private final @Getter OrderBySource orderBySource;
		private boolean alwaysIgnoreCase;

		public Predicate(String predicate, Class<?> domainClass) {

			String[] parts = split(detectAndSetAllIgnoreCase(predicate), ORDER_BY);

			if (parts.length > 2) {
				throw new IllegalArgumentException("OrderBy must not be used more than once in a method name!");
			}

			this.nodes = Arrays.stream(split(parts[0], "Or"))//
					.map(part -> new OrPart(part, domainClass, alwaysIgnoreCase))//
					.collect(Collectors.toList());

			this.orderBySource = parts.length == 2 ? new OrderBySource(parts[1], Optional.of(domainClass))
					: OrderBySource.EMPTY;
		}

		private String detectAndSetAllIgnoreCase(String predicate) {

			Matcher matcher = ALL_IGNORE_CASE.matcher(predicate);

			if (matcher.find()) {
				alwaysIgnoreCase = true;
				predicate = predicate.substring(0, matcher.start()) + predicate.substring(matcher.end(), predicate.length());
			}

			return predicate;
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<OrPart> iterator() {
			return nodes.iterator();
		}
	}
}
