package org.springframework.data.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Utility methods to work with {@link Stream} and {@link Page}s. </br>
 * It has been thought to provide some writing like that: </br>
 * </br>
 * <code>
 * class DatasProvider {</br>
 *  	public Page<T> getDatasPage() {</br>
 *  		Collection<T> items = [FILL THE ITEMS];</br>
 *  		return items.stream().map(...).filter(...).collector(PageCollectors.toSimplePage());</br>
 *  	}</br>
 *  }</br>
 * </code></br>
 * or</br>
 * <code>
 * class DatasProvider {</br>
 *  	public Page<T> getDatasPage() {</br>
 *  		Page<T> page = repo.[ANY PAGED SEARCH];</br>
 *  		return page.stream().map(...).filter(...).collector(PageCollectors.toSimplePage());</br>
 *  	}</br>
 *  }</br>
 * </code></br>
 * or</br>
 * <code>
 *  class DatasProvider {</br>
 *  	public Page<T> getDatasPage(Pageable pageable) {</br>
 *  		Collection<T> items = [FILL THE ITEMS];</br>
 *  		return items.stream().map(...).filter(...).collector(PageCollectors.toPage(pageable));</br>
 *  	}</br>
 *  }</br>
 *  </code>
 *
 * @author Bertrand Moreau
 */
public final class PageCollectors<T> {

	private static final Set<Characteristics> characteristics = Collections.emptySet();

	/**
	 * Simply put all the {@link Stream} data into a {@link Page}.<strong>Use is IF the {@link Page} is already returned
	 * by the repo BUT processed after</strong>
	 *
	 * @param p
	 * @param <T>
	 * @return a {@link Page} containing all the {@link Stream} data and the {@link Pageable} informations.
	 */
	public static <T> Collector<T, List<T>, Page<T>> toSimplePage(final Pageable p) {
		return new SimplePageCollectorImpl<>(p);
	}

	/**
	 * Simply put all the {@link Stream} data into a {@link Page}.
	 *
	 * @param <T>
	 * @return a {@link Page} containing all the {@link Stream} datas
	 */
	public static <T> Collector<T, List<T>, Page<T>> toSimplePage() {
		return new SimplePageCollectorImpl<>();
	}

	private static class SimplePageCollectorImpl<T> implements Collector<T, List<T>, Page<T>> {

		private Pageable p = null;

		public SimplePageCollectorImpl() {}

		public SimplePageCollectorImpl(final Pageable p) {
			this.p = Objects.requireNonNull(p);
		}

		@Override
		public Set<Characteristics> characteristics() {
			return characteristics;
		}

		@Override
		public Supplier<List<T>> supplier() {
			return ArrayList::new;
		}

		@Override
		public BiConsumer<List<T>, T> accumulator() {
			return List::add;
		}

		@Override
		public BinaryOperator<List<T>> combiner() {
			return (left, right) -> Stream.of(left, right).flatMap(Collection::stream).collect(Collectors.toList());
		}

		@Override
		public Function<List<T>, Page<T>> finisher() {
			return t -> p == null ? new PageImpl<>(t) : new PageImpl<>(t, p, t.size());
		}

	}

	/**
	 * Reduce the {@link Stream} as {@link Page} based on the {@link Pageable} information.
	 *
	 * @param <T>
	 * @param p
	 * @return a {@link Page} containing a subset of the {@link Stream}
	 */
	public static <T> Collector<T, List<T>, Page<T>> toPage(final Pageable p) {
		return new PageCollectorImpl<>(p);
	}

	private static class PageCollectorImpl<T> implements Collector<T, List<T>, Page<T>> {

		private final Pageable p;

		public PageCollectorImpl(final Pageable p) {
			this.p = Objects.requireNonNull(p);
		}

		@Override
		public Set<Characteristics> characteristics() {
			return characteristics;
		}

		@Override
		public Supplier<List<T>> supplier() {
			return ArrayList::new;
		}

		@Override
		public BiConsumer<List<T>, T> accumulator() {
			return List::add;
		}

		@Override
		public BinaryOperator<List<T>> combiner() {
			return (left, right) -> Stream.of(left, right).flatMap(Collection::stream).collect(Collectors.toList());
		}

		@Override
		public Function<List<T>, Page<T>> finisher() {
			return t -> {
				final int pageNumber = p.getPageNumber();
				final int pageSize = p.getPageSize();
				final int fromIndex = Math.min(t.size(), pageNumber * pageSize);
				final int toIndex = Math.min(t.size(), (pageNumber + 1) * pageSize);

				return new PageImpl<>(t.subList(fromIndex, toIndex), p, t.size());
			};
		}

	}

	/**
	 * Reduce the {@link Stream} as {@link Page} based on the {@link Pageable} information.
	 *
	 * @param <T>
	 * @param p
	 * @return a {@link Page} containing a subset of the {@link Stream} sort following the {@link Comparator}
	 */
	public static <T> Collector<T, List<T>, Page<T>> toSortedPage(final Pageable p, final Comparator<T> c) {
		return new SortedPageCollectorImpl<>(p, c);
	}

	private static class SortedPageCollectorImpl<T> implements Collector<T, List<T>, Page<T>> {

		private final Pageable p;
		private final Comparator<T> c;

		public SortedPageCollectorImpl(final Pageable p, final Comparator<T> c) {
			this.p = Objects.requireNonNull(p);
			this.c = Objects.requireNonNull(c);
		}

		@Override
		public Set<Characteristics> characteristics() {
			return characteristics;
		}

		@Override
		public Supplier<List<T>> supplier() {
			return ArrayList::new;
		}

		@Override
		public BiConsumer<List<T>, T> accumulator() {
			return List::add;
		}

		@Override
		public BinaryOperator<List<T>> combiner() {
			return (left, right) -> Stream.of(left, right).flatMap(Collection::stream).collect(Collectors.toList());
		}

		@Override
		public Function<List<T>, Page<T>> finisher() {
			return t -> {
				final int pageNumber = p.getPageNumber();
				final int pageSize = p.getPageSize();
				final int fromIndex = Math.min(t.size(), pageNumber * pageSize);
				final int toIndex = Math.min(t.size(), (pageNumber + 1) * pageSize);

				final List<T> data = t.subList(fromIndex, toIndex);
				data.sort(c);

				return new PageImpl<>(data, p, t.size());
			};
		}

	}

	/**
	 * Reduce the {@link Stream} as {@link Page} based on the {@link Pageable} information. <br>
	 * <strong>The {@link Stream} is filtered before subset of data isolated</strong>
	 *
	 * @param <T>
	 * @param p
	 * @return a {@link Page} containing a subset of the {@link Stream}
	 */
	public static <T> Collector<T, List<T>, Page<T>> toFilteredPage(final Pageable p, final Predicate<T> f) {
		return new FilteredPageCollectorImpl<>(p, f);
	}

	private static class FilteredPageCollectorImpl<T> implements Collector<T, List<T>, Page<T>> {

		private final Pageable p;
		private final Predicate<T> f;

		public FilteredPageCollectorImpl(final Pageable p, final Predicate<T> f) {
			this.p = Objects.requireNonNull(p);
			this.f = Objects.requireNonNull(f);
		}

		@Override
		public Set<Characteristics> characteristics() {
			return characteristics;
		}

		@Override
		public Supplier<List<T>> supplier() {
			return ArrayList::new;
		}

		@Override
		public BiConsumer<List<T>, T> accumulator() {
			return List::add;
		}

		@Override
		public BinaryOperator<List<T>> combiner() {
			return (left, right) -> Stream.of(left, right).flatMap(Collection::stream).collect(Collectors.toList());
		}

		@Override
		public Function<List<T>, Page<T>> finisher() {
			return t -> {
				final List<T> data = t.stream().filter(f).collect(Collectors.toList());

				final int pageNumber = p.getPageNumber();
				final int pageSize = p.getPageSize();
				final int fromIndex = Math.min(data.size(), pageNumber * pageSize);
				final int toIndex = Math.min(data.size(), (pageNumber + 1) * pageSize);

				return new PageImpl<>(data.subList(fromIndex, toIndex), p, t.size());
			};
		}

	}

	/**
	 * Reduce the {@link Stream} as {@link Page} based on the {@link Pageable} information. <br>
	 * <strong>The {@link Stream} is filtered then sorted then the subset of data isolated</strong>
	 *
	 * @param <T>
	 * @param p
	 * @return a {@link Page} containing a subset of the {@link Stream}
	 */
	public static <T> Collector<T, List<T>, Page<T>> toFilteredSortedPage(final Pageable p, final Predicate<T> f,
			final Comparator<T> c) {
		return new FilteredSortedPageCollectorImpl<>(p, f, c);
	}

	private static class FilteredSortedPageCollectorImpl<T> implements Collector<T, List<T>, Page<T>> {

		private final Pageable p;
		private final Predicate<T> f;
		private final Comparator<T> c;

		public FilteredSortedPageCollectorImpl(final Pageable p, final Predicate<T> f, final Comparator<T> c) {
			this.p = Objects.requireNonNull(p);
			this.f = Objects.requireNonNull(f);
			this.c = Objects.requireNonNull(c);
		}

		@Override
		public Set<Characteristics> characteristics() {
			return characteristics;
		}

		@Override
		public Supplier<List<T>> supplier() {
			return ArrayList::new;
		}

		@Override
		public BiConsumer<List<T>, T> accumulator() {
			return List::add;
		}

		@Override
		public BinaryOperator<List<T>> combiner() {
			return (left, right) -> Stream.of(left, right).flatMap(Collection::stream).collect(Collectors.toList());
		}

		@Override
		public Function<List<T>, Page<T>> finisher() {
			return t -> {
				final List<T> data = t.stream().filter(f).sorted(c).collect(Collectors.toList());

				final int pageNumber = p.getPageNumber();
				final int pageSize = p.getPageSize();
				final int fromIndex = Math.min(data.size(), pageNumber * pageSize);
				final int toIndex = Math.min(data.size(), (pageNumber + 1) * pageSize);

				return new PageImpl<>(data.subList(fromIndex, toIndex), p, t.size());
			};
		}

	}

}
