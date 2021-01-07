/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.spel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.data.util.Streamable;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Value object capturing dependencies to a method or property/field that is referenced from a SpEL expression.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.4
 */
public class ExpressionDependencies implements Streamable<ExpressionDependencies.ExpressionDependency> {

	private static final ExpressionDependencies EMPTY = new ExpressionDependencies(Collections.emptyList());

	private final List<ExpressionDependency> dependencies;

	private ExpressionDependencies(List<ExpressionDependency> dependencies) {
		this.dependencies = dependencies;
	}

	/**
	 * Return an empty {@link ExpressionDependencies} object.
	 *
	 * @return empty dependencies.
	 */
	public static ExpressionDependencies none() {
		return EMPTY;
	}

	/**
	 * Return an {@link ExpressionDependencies} object representing the given {@link Collection} of
	 * {@link ExpressionDependency dependencies}.
	 *
	 * @return {@link ExpressionDependencies} holding the given {@link ExpressionDependency dependencies} or
	 *         {@link ExpressionDependencies#none() none} if the collection is {@link Collection#isEmpty() empty}.
	 */
	public static ExpressionDependencies of(Collection<ExpressionDependency> dependencies) {

		if (dependencies.isEmpty()) {
			return EMPTY;
		}

		return new ExpressionDependencies(new ArrayList<>(new LinkedHashSet<>(dependencies)));
	}

	/**
	 * Return an {@link ExpressionDependencies} object representing the merged collection of {@link ExpressionDependency
	 * dependencies} withing the given {@link ExpressionDependencies} collection.
	 *
	 * @return {@link ExpressionDependencies} holding a set of merged {@link ExpressionDependency dependencies} or
	 *         {@link ExpressionDependencies#none() none} if the given {@link Iterable} is {@link Collection#isEmpty()
	 *         empty}.
	 */
	public static ExpressionDependencies merged(Iterable<ExpressionDependencies> dependencies) {

		if (!dependencies.iterator().hasNext()) {
			return EMPTY;
		}

		List<ExpressionDependency> dependencySet = new ArrayList<>();
		dependencies.forEach(it -> dependencySet.addAll(it.dependencies));
		return ExpressionDependencies.of(dependencySet);
	}

	/**
	 * Discover all expression dependencies that are referenced in the {@link SpelNode expression root}.
	 *
	 * @param expression the SpEL expression to inspect.
	 * @return a set of {@link ExpressionDependencies}.
	 */
	public static ExpressionDependencies discover(Expression expression) {
		return expression instanceof SpelExpression ? discover(((SpelExpression) expression).getAST(), true) : none();
	}

	/**
	 * Discover all expression dependencies that are referenced in the {@link SpelNode expression root}.
	 *
	 * @param root the SpEL expression to inspect.
	 * @param topLevelOnly whether to include top-level dependencies only. Top-level dependencies are dependencies that
	 *          indicate the start of a compound expression and required to resolve the next expression item.
	 * @return a set of {@link ExpressionDependencies}.
	 */
	public static ExpressionDependencies discover(SpelNode root, boolean topLevelOnly) {

		List<ExpressionDependency> dependencies = new ArrayList<>();

		collectDependencies(root, 0, expressionDependency -> {
			if (!topLevelOnly || expressionDependency.isTopLevel()) {
				dependencies.add(expressionDependency);
			}
		});

		return new ExpressionDependencies(dependencies);
	}

	private static void collectDependencies(SpelNode node, int compoundPosition,
			Consumer<ExpressionDependency> dependencies) {

		if (node instanceof MethodReference) {
			dependencies.accept(ExpressionDependency.forMethod(((MethodReference) node).getName()).nest(compoundPosition));
		}

		if (node instanceof PropertyOrFieldReference) {
			dependencies.accept(
					ExpressionDependency.forPropertyOrField(((PropertyOrFieldReference) node).getName()).nest(compoundPosition));
		}

		for (int i = 0; i < node.getChildCount(); i++) {
			collectDependencies(node.getChild(i), node instanceof CompoundExpression ? i : 0, dependencies);
		}
	}

	/**
	 * Create new {@link ExpressionDependencies} that contains all dependencies from this object and {@code other}. The
	 * merged dependencies are guaranteed to not contain duplicates.
	 *
	 * @param other the other {@link ExpressionDependencies} object.
	 * @return new merged {@link ExpressionDependencies} object.
	 */
	public ExpressionDependencies mergeWith(ExpressionDependencies other) {

		Assert.notNull(other, "Other ExpressionDependencies must not be null");

		Set<ExpressionDependency> dependencySet = new LinkedHashSet<>(this.dependencies.size() + other.dependencies.size());
		dependencySet.addAll(this.dependencies);
		dependencySet.addAll(other.dependencies);

		return new ExpressionDependencies(new ArrayList<>(dependencySet));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<ExpressionDependency> iterator() {
		return this.dependencies.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ExpressionDependencies)) {
			return false;
		}
		ExpressionDependencies that = (ExpressionDependencies) o;
		return ObjectUtils.nullSafeEquals(dependencies, that.dependencies);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(dependencies);
	}

	/**
	 * Value object to describe a dependency to a method or property/field that is referenced from a SpEL expression.
	 *
	 * @author Mark Paluch
	 * @since 2.4
	 */
	public static class ExpressionDependency {

		private final DependencyType type;
		private final String symbol;
		private final int nestLevel;

		private ExpressionDependency(DependencyType type, String symbol, int nestLevel) {

			this.symbol = symbol;
			this.nestLevel = nestLevel;
			this.type = type;
		}

		/**
		 * Create a new {@link ExpressionDependency} for a method.
		 *
		 * @param methodName the method name.
		 * @return a method dependency on {@code methodName}.
		 */
		public static ExpressionDependency forMethod(String methodName) {
			return new ExpressionDependency(DependencyType.METHOD, methodName, 0);
		}

		/**
		 * Create a new {@link ExpressionDependency} for a property or field.
		 *
		 * @param fieldOrPropertyName the property/field name.
		 * @return a method dependency on {@code fieldOrPropertyName}.
		 */
		public static ExpressionDependency forPropertyOrField(String fieldOrPropertyName) {
			return new ExpressionDependency(DependencyType.PROPERTY, fieldOrPropertyName, 0);
		}

		/**
		 * Associate a nesting {@code level} with the {@link ExpressionDependency}. Returns
		 *
		 * @param level
		 * @return
		 */
		public ExpressionDependency nest(int level) {
			return nestLevel == level ? this : new ExpressionDependency(type, symbol, level);
		}

		public boolean isNested() {
			return !isTopLevel();
		}

		public boolean isTopLevel() {
			return this.nestLevel == 0;
		}

		public boolean isMethod() {
			return this.type == DependencyType.METHOD;
		}

		public boolean isPropertyOrField() {
			return this.type == DependencyType.PROPERTY;
		}

		public String getSymbol() {
			return symbol;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ExpressionDependency)) {
				return false;
			}
			ExpressionDependency that = (ExpressionDependency) o;
			if (nestLevel != that.nestLevel) {
				return false;
			}
			if (type != that.type) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(symbol, that.symbol);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(type);
			result = 31 * result + ObjectUtils.nullSafeHashCode(symbol);
			result = 31 * result + nestLevel;
			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "ExpressionDependency{" + "type=" + type + ", symbol='" + symbol + '\'' + ", nestLevel=" + nestLevel + '}';
		}

		enum DependencyType {
			PROPERTY, METHOD;
		}
	}
}
