package org.springframework.data.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

/**
 * Test methods for {@link PageCollectors}s.
 *
 * @author Bertrand Moreau
 */
public class PageCollectorToSimplePageTest {

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
		final Page<Integer> page = ints.stream().collect(PageCollectors.toSimplePage());

		assertEquals(size, page.getSize());
		assertEquals(size, page.getContent().size());
	}

	@Test
	void emptyPage() {
		final Page<Object> page = Collections.emptyList().stream().collect(PageCollectors.toSimplePage());

		assertEquals(0, page.getSize());
		assertEquals(0, page.getContent().size());
	}

	@Test
	void checkData() {
		final List<String> datas = Arrays.asList("un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
				"dix");

		final int size = datas.size();
		final Page<String> page = datas.stream().collect(PageCollectors.toSimplePage());

		assertEquals(size, page.getSize());
		assertEquals(size, page.getContent().size());
		assertArrayEquals(datas.toArray(), page.getContent().toArray());
	}

}
