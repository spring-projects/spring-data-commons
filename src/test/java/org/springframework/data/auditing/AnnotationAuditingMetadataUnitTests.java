/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.auditing;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.util.ReflectionUtils;

/**
 * Unit test for {@link org.springframework.data.auditing.AnnotationAuditingMetadata}.
 * 
 * @author Ranie Jade Ramiso
 * @author Oliver Gierke
 * @since 1.5
 */
public class AnnotationAuditingMetadataUnitTests {

	static final Field createdByField = ReflectionUtils.findField(AnnotatedUser.class, "createdBy");
	static final Field createdDateField = ReflectionUtils.findField(AnnotatedUser.class, "createdDate");
	static final Field lastModifiedByField = ReflectionUtils.findField(AnnotatedUser.class, "lastModifiedBy");
	static final Field lastModifiedDateField = ReflectionUtils.findField(AnnotatedUser.class, "lastModifiedDate");

	@Rule public ExpectedException exception = ExpectedException.none();

	@Test
	public void checkAnnotationDiscovery() {

		AnnotationAuditingMetadata metadata = AnnotationAuditingMetadata.getMetadata(AnnotatedUser.class);

		assertThat(metadata).isNotNull();
		assertThat(metadata.getCreatedByField()).hasValue(createdByField);
		assertThat(metadata.getCreatedDateField()).hasValue(createdDateField);
		assertThat(metadata.getLastModifiedByField()).hasValue(lastModifiedByField);
		assertThat(metadata.getLastModifiedDateField()).hasValue(lastModifiedDateField);
	}

	@Test
	public void checkCaching() {

		AnnotationAuditingMetadata firstCall = AnnotationAuditingMetadata.getMetadata(AnnotatedUser.class);
		assertThat(firstCall).isNotNull();

		AnnotationAuditingMetadata secondCall = AnnotationAuditingMetadata.getMetadata(AnnotatedUser.class);
		assertThat(firstCall).isEqualTo(secondCall);
	}

	@Test
	public void checkIsAuditable() {

		AnnotationAuditingMetadata metadata = AnnotationAuditingMetadata.getMetadata(AnnotatedUser.class);
		assertThat(metadata).isNotNull();
		assertThat(metadata.isAuditable()).isTrue();

		metadata = AnnotationAuditingMetadata.getMetadata(NonAuditableUser.class);
		assertThat(metadata).isNotNull();
		assertThat(metadata.isAuditable()).isFalse();
	}

	@Test
	public void rejectsInvalidDateTypeField() {

		class Sample {
			@CreatedDate String field;
		}

		exception.expect(IllegalStateException.class);
		exception.expectMessage(String.class.getName());
		exception.expectMessage("field");

		AnnotationAuditingMetadata.getMetadata(Sample.class);
	}

	@SuppressWarnings("unused")
	static class NonAuditableUser {
		private Object nonAuditProperty;
	}
}
