package org.springframework.data.domain.domain;

import static org.junit.Assert.*;

import java.lang.reflect.Field;

import org.junit.Test;
import org.springframework.data.domain.audit.*;
import org.springframework.util.ReflectionUtils;

/**
 * Unit test for {@link org.springframework.data.domain.audit.AuditableMetadata}
 *
 * @author Ranie Jade Ramiso
 */
public class AuditableMetadataUnitTest {
	private class User {
		@CreatedBy
		private Object createdBy;

		@CreatedDate
		private Object createdDate;

		@LastModifiedBy
		private Object lastModifiedBy;

		@LastModifiedDate
		private Object lastModifiedDate;
	}

	private class NonAuditableUser {
		private Object nonAuditProperty;
	}

	private static final Field createdByField = ReflectionUtils.findField(User.class, "createdBy");
	private static final Field createdDateField = ReflectionUtils.findField(User.class, "createdDate");
	private static final Field lastModifiedByField = ReflectionUtils.findField(User.class, "lastModifiedBy");
	private static final Field lastModifiedDateField = ReflectionUtils.findField(User.class, "lastModifiedDate");

	@Test
	public void checkAnnotationDiscovery() {
		AuditableMetadata metadata = AuditableMetadata.getMetadata(User.class);
		assertNotNull(metadata);

		assertEquals(createdByField, metadata.getCreatedByField());
		assertEquals(createdDateField, metadata.getCreatedDateField());
		assertEquals(lastModifiedByField, metadata.getLastModifiedByField());
		assertEquals(lastModifiedDateField, metadata.getLastModifiedDateField());
	}

	@Test
	public void checkCaching() {
		AuditableMetadata firstCall = AuditableMetadata.getMetadata(User.class);
		assertNotNull(firstCall);

		AuditableMetadata secondCall = AuditableMetadata.getMetadata(User.class);
		assertEquals(firstCall, secondCall);
	}

	@Test
	public void checkIsAuditable() {
		AuditableMetadata metadata = AuditableMetadata.getMetadata(User.class);
		assertNotNull(metadata);

		assertTrue(metadata.isAuditable());

		metadata = AuditableMetadata.getMetadata(NonAuditableUser.class);
		assertNotNull(metadata);

		assertFalse(metadata.isAuditable());
	}
}
