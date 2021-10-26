package org.springframework.data.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Test methods for {@link PageCollectors}s.
 *
 * @author Bertrand Moreau
 */
public class PageCollectorsToFilteredSortedPageTest {

	private List<Integer> ints;
	private int size;

	@BeforeEach
	void init() {
		final Random rand = new Random();
		size = rand.nextInt(10000);
		ints = IntStream.range(0, size).mapToObj(i -> rand.nextInt()).collect(Collectors.toList());
	}

	@Test
	void fullPage() {
		final Pageable pageable = Pageable.ofSize(size);
		final Page<Integer> page = ints.stream()
				.collect(PageCollectors.toFilteredSortedPage(pageable, i -> i > 0, Integer::compare));

		assertEquals(size, page.getSize());
		assertTrue(page.getContent().size() <= size);

		final List<Integer> content = page.getContent();
		for (final Integer element : content) {
			assertTrue(element > 0);
		}

		for (int j = 1; j < content.size(); j++) {
			assertTrue(Integer.compare(content.get(j - 1), content.get(j)) < 0);
		}
	}

	@Test
	void emptyPage() {
		final Pageable pageable = Pageable.ofSize(size);
		final Page<Integer> page = Collections.<Integer>emptyList().stream()
				.collect(PageCollectors.toFilteredSortedPage(pageable, i -> i > 0, Integer::compare));

		assertEquals(size, page.getSize());
		assertTrue(page.getContent().isEmpty());
	}

	@Test
	void secondPage() {
		final Pageable pageable = Pageable.ofSize(size / 4).withPage(2);
		final Page<Integer> page = ints.stream()
				.collect(PageCollectors.toFilteredSortedPage(pageable, i -> i < 0, Integer::compare));

		assertEquals(size / 4, page.getSize());
		assertTrue(page.getContent().size() <= size);

		final List<Integer> content = page.getContent();
		for (final Integer element : content) {
			assertTrue(element < 0);
		}

		for (int j = 1; j < content.size(); j++) {
			assertTrue(Integer.compare(content.get(j - 1), content.get(j)) < 0);
		}
	}

	@Test
	void checkData() {
		final List<String> datas = Arrays.asList("un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
				"dix");

		final int size = datas.size();
		final Pageable pageable = Pageable.ofSize(size / 2).withPage(0);
		final Page<String> page = datas.stream()
				.collect(PageCollectors.toFilteredSortedPage(pageable, t -> t.contains("i"), String::compareTo));

		assertEquals(size / 2, page.getSize());
		assertEquals(size / 2, page.getContent().size());
		for (final String string : page.getContent()) {
			assertTrue(string.contains("i"));
			assertFalse(!string.contains("i"));
		}
		assertIterableEquals(page.getContent(), Arrays.asList("cinq", "dix", "huit", "six", "trois"));
	}

}
