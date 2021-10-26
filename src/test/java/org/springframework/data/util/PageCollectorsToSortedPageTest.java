package org.springframework.data.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
public class PageCollectorsToSortedPageTest {

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
		final Page<Integer> page = ints.stream().collect(PageCollectors.toSortedPage(pageable, Integer::compare));

		assertEquals(size, page.getSize());
		assertEquals(size, page.getContent().size());

		final List<Integer> content = page.getContent();
		for (int i = 1; i < size; i++) {
			assertTrue(Integer.compare(content.get(i - 1), content.get(i)) < 0);
		}
	}

	@Test
	void emptyPage() {
		final Pageable pageable = Pageable.ofSize(size);
		final Page<Integer> page = Collections.<Integer> emptyList().stream()
				.collect(PageCollectors.toSortedPage(pageable, Integer::compare));

		assertEquals(size, page.getSize());
		assertTrue(page.getContent().isEmpty());
	}

	@Test
	void secondPage() {
		final Pageable pageable = Pageable.ofSize(size / 4).withPage(2);
		final Page<Integer> page = ints.stream().collect(PageCollectors.toSortedPage(pageable, Integer::compare));

		assertEquals(size / 4, page.getSize());
		assertEquals(size / 4, page.getContent().size());
		final List<Integer> content = page.getContent();
		for (int i = 1; i < content.size(); i++) {
			assertTrue(Integer.compare(content.get(i - 1), content.get(i)) < 0);
		}
	}

	@Test
	void checkData() {
		final List<String> datas = Arrays.asList("un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
				"dix");

		final int size = datas.size();
		final Pageable pageable = Pageable.ofSize(size / 2).withPage(1);
		final Page<String> page = datas.stream().collect(PageCollectors.toSortedPage(pageable, String::compareTo));

		assertEquals(size / 2, page.getSize());
		assertEquals(size / 2, page.getContent().size());
		assertIterableEquals(page.getContent(), Arrays.asList("dix", "huit", "neuf", "sept", "six"));
	}

}
