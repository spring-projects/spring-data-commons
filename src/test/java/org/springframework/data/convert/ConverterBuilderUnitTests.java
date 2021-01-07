/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.convert;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mockito.internal.util.Supplier;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.data.convert.ConverterBuilder.ConverterAware;
import org.springframework.data.convert.ConverterBuilder.ReadingConverterBuilder;
import org.springframework.data.convert.ConverterBuilder.WritingConverterBuilder;

/**
 * Unit tests for {@link DefaultConverterBuilder}.
 *
 * @author Oliver Gierke
 * @since 2.0
 * @soundtrack John Mayer - In the Blood (The Search for Everything)
 */
class ConverterBuilderUnitTests {

	@Test // DATACMNS-1034
	void setsUpBidirectionalConvertersFromReading() {

		ConverterAware builder = ConverterBuilder.reading(String.class, Long.class, it -> Long.valueOf(it))
				.andWriting(Object::toString);

		assertConverter(builder.getReadingConverter(), "1", 1L);
		assertConverter(builder.getWritingConverter(), 1L, "1");
	}

	@Test // DATACMNS-1034
	void setsUpBidirectionalConvertersFromWriting() {

		ConverterAware builder = ConverterBuilder.writing(Long.class, String.class, Object::toString)
				.andReading(it -> Long.valueOf(it));

		assertConverter(builder.getReadingConverter(), "1", 1L);
		assertConverter(builder.getWritingConverter(), 1L, "1");
	}

	@Test // DATACMNS-1034
	void setsUpReadingConverter() {

		ReadingConverterBuilder<String, Long> builder = ConverterBuilder.reading(String.class, Long.class,
				string -> Long.valueOf(string));

		assertConverter(builder.getReadingConverter(), "1", 1L);
		assertOnlyConverter(builder, builder::getReadingConverter);
	}

	@Test // DATACMNS-1034
	void setsUpWritingConverter() {

		WritingConverterBuilder<Long, String> builder = ConverterBuilder.writing(Long.class, String.class,
				Object::toString);

		assertConverter(builder.getWritingConverter(), 1L, "1");
		assertOnlyConverter(builder, builder::getWritingConverter);
	}

	private static void assertConverter(GenericConverter converter, Object source, Object target) {

		assertThat(converter.getConvertibleTypes())
				.containsExactly(new ConvertiblePair(source.getClass(), target.getClass()));
		assertThat(converter.convert(source, TypeDescriptor.forObject(source), TypeDescriptor.forObject(target)))
				.isEqualTo(target);
	}

	private static void assertOnlyConverter(ConverterBuilder builder, Supplier<GenericConverter> supplier) {
		assertThat(builder.getConverters()).containsExactly(supplier.get());
	}
}
