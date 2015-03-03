/*
 * Copyright 2008-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
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
 */
public class PartTree implements Iterable<OrPart> {

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
	private static final String DELETE_PATTERN = "delete|remove";
	private static final Pattern PREFIX_TEMPLATE = Pattern.compile( //
			"^(" + QUERY_PATTERN + "|" + COUNT_PATTERN + "|" + DELETE_PATTERN + ")((\\p{Lu}.*?))??By");

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
			this.subject = new Subject(null);
			this.predicate = new Predicate(source, domainClass);
		} else {
			this.subject = new Subject(matcher.group(0));
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

		OrderBySource orderBySource = predicate.getOrderBySource();
		return orderBySource == null ? null : orderBySource.toSort();
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
	public Boolean isCountProjection() {
		return subject.isCountProjection();
	}

	/**
	 * return true if the created {@link PartTree} is meant to be used for delete operation.
	 * 
	 * @return
	 * @since 1.8
	 */
	public Boolean isDelete() {
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
		return subject.getMaxResults();
	}

	/**
	 * Returns an {@link Iterable} of all parts contained in the {@link PartTree}.
	 * 
	 * @return the iterable {@link Part}s
	 */
	public Iterable<Part> getParts() {

		List<Part> result = new ArrayList<Part>();
		for (OrPart orPart : this) {
			for (Part part : orPart) {
				result.add(part);
			}
		}
		return result;
	}

	/**
	 * Returns all {@link Part}s of the {@link PartTree} of the given {@link Type}.
	 * 
	 * @param type
	 * @return
	 */
	public Iterable<Part> getParts(Type type) {

		List<Part> result = new ArrayList<Part>();

		for (Part part : getParts()) {
			if (part.getType().equals(type)) {
				result.add(part);
			}
		}

		return result;
	}

	@Override
	public String toString() {

		OrderBySource orderBySource = predicate.getOrderBySource();
		return String.format("%s%s", StringUtils.collectionToDelimitedString(predicate.nodes, " or "),
				orderBySource == null ? "" : " " + orderBySource);
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
	public static class OrPart implements Iterable<Part> {

		private final List<Part> children = new ArrayList<Part>();

		/**
		 * Creates a new {@link OrPart}.
		 * 
		 * @param source the source to split up into {@literal And} parts in turn.
		 * @param domainClass the domain class to check the resulting {@link Part}s against.
		 * @param alwaysIgnoreCase if always ignoring case
		 */
		OrPart(String source, Class<?> domainClass, boolean alwaysIgnoreCase) {

			String[] split = split(source, "And");
			for (String part : split) {
				if (StringUtils.hasText(part)) {
					children.add(new Part(part, domainClass, alwaysIgnoreCase));
				}
			}
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
		private static final Pattern DELETE_BY_TEMPLATE = Pattern.compile("^(" + DELETE_PATTERN + ")(\\p{Lu}.*?)??By");
		private static final String LIMITING_QUERY_PATTERN = "(First|Top)(\\d*)?";
		private static final Pattern LIMITED_QUERY_TEMPLATE = Pattern.compile("^(" + QUERY_PATTERN + ")(" + DISTINCT + ")?"
				+ LIMITING_QUERY_PATTERN + "(\\p{Lu}.*?)??By");

		private final boolean distinct;
		private final boolean count;
		private final boolean delete;
		private final Integer maxResults;

		public Subject(String subject) {

			this.distinct = subject == null ? false : subject.contains(DISTINCT);
			this.count = matches(subject, COUNT_BY_TEMPLATE);
			this.delete = matches(subject, DELETE_BY_TEMPLATE);
			this.maxResults = returnMaxResultsIfFirstKSubjectOrNull(subject);
		}

		/**
		 * @param subject
		 * @return
		 * @since 1.9
		 */
		private Integer returnMaxResultsIfFirstKSubjectOrNull(String subject) {

			if (subject == null) {
				return null;
			}

			Matcher grp = LIMITED_QUERY_TEMPLATE.matcher(subject);

			if (!grp.find()) {
				return null;
			}

			return StringUtils.hasText(grp.group(4)) ? Integer.valueOf(grp.group(4)) : 1;
		}

		/**
		 * Returns {@literal true} if {@link Subject} matches {@link #DELETE_BY_TEMPLATE}.
		 * 
		 * @return
		 * @since 1.8
		 */
		public Boolean isDelete() {
			return delete;
		}

		public boolean isCountProjection() {
			return count;
		}

		public boolean isDistinct() {
			return distinct;
		}

		public Integer getMaxResults() {
			return maxResults;
		}

		private final boolean matches(String subject, Pattern pattern) {
			return subject == null ? false : pattern.matcher(subject).find();
		}
	}

	/**
	 * Represents the predicate part of the query.
	 * 
	 * @author Oliver Gierke
	 * @author Phil Webb
	 */
	private static class Predicate {

		private static final Pattern ALL_IGNORE_CASE = Pattern.compile("AllIgnor(ing|e)Case");
		private static final String ORDER_BY = "OrderBy";

		private final List<OrPart> nodes = new ArrayList<OrPart>();
		private final OrderBySource orderBySource;
		private boolean alwaysIgnoreCase;

		public Predicate(String predicate, Class<?> domainClass) {

			String[] parts = split(detectAndSetAllIgnoreCase(predicate), ORDER_BY);

			if (parts.length > 2) {
				throw new IllegalArgumentException("OrderBy must not be used more than once in a method name!");
			}

			buildTree(parts[0], domainClass);
			this.orderBySource = parts.length == 2 ? new OrderBySource(parts[1], domainClass) : null;
		}

		private String detectAndSetAllIgnoreCase(String predicate) {

			Matcher matcher = ALL_IGNORE_CASE.matcher(predicate);

			if (matcher.find()) {
				alwaysIgnoreCase = true;
				predicate = predicate.substring(0, matcher.start()) + predicate.substring(matcher.end(), predicate.length());
			}

			return predicate;
		}

		private void buildTree(String source, Class<?> domainClass) {

			String[] split = split(source, "Or");
			for (String part : split) {
				nodes.add(new OrPart(part, domainClass, alwaysIgnoreCase));
			}
		}

		public Iterator<OrPart> iterator() {
			return nodes.iterator();
		}

		public OrderBySource getOrderBySource() {
			return orderBySource;
		}
	}
}
