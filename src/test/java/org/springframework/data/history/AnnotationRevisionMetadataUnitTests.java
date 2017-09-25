/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.history;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Reference;

/**
 * Unit tests for {@link AnnotationRevisionMetadata}.
 * 
 * @author Oliver Gierke
 */
public class AnnotationRevisionMetadataUnitTests {

	@Test // DATACMNS-1173
	public void exposesNoInformationOnEmptyProbe() {

		Sample sample = new Sample();
		RevisionMetadata<Long> metadata = getMetadata(sample);

		assertThat(metadata.getRevisionNumber()).isEmpty();
		assertThat(metadata.getRevisionDate()).isEmpty();

		assertThatExceptionOfType(IllegalStateException.class) //
				.isThrownBy(() -> metadata.getRequiredRevisionNumber());

		assertThatExceptionOfType(IllegalStateException.class) //
				.isThrownBy(() -> metadata.getRequiredRevisionDate());
	}

	@Test // DATACMNS-1173
	public void exposesRevisionNumber() {

		Sample sample = new Sample();
		sample.revisionNumber = 1L;

		RevisionMetadata<Long> metadata = getMetadata(sample);

		assertThat(metadata.getRevisionNumber()).hasValue(1L);
		assertThat(metadata.getRequiredRevisionNumber()).isEqualTo(1L);
	}

	@Test // DATACMNS-1173
	public void exposesRevisionDate() {

		Sample sample = new Sample();
		sample.revisionDate = LocalDateTime.now();

		RevisionMetadata<Long> metadata = getMetadata(sample);

		assertThat(metadata.getRevisionDate()).hasValue(sample.revisionDate);
		assertThat(metadata.getRequiredRevisionDate()).isEqualTo(sample.revisionDate);
	}

	private static RevisionMetadata<Long> getMetadata(Sample sample) {
		return new AnnotationRevisionMetadata<>(sample, Autowired.class, Reference.class);
	}

	static class Sample {

		@Autowired Long revisionNumber;
		@Reference LocalDateTime revisionDate;
	}
}
