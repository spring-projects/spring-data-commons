package org.springframework.data.domain.audit;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.ReflectionUtils;

/**
 * Inspects the given {@link Class} for fields annotated by {@link CreatedBy}, {@link CreatedDate},
 * {@link LastModifiedBy} , and {@link LastModifiedDate}. Only one field per annotation is stored.
 * 
 * @author Ranie Jade Ramiso
 */
public class AuditableMetadata {
	private static final Map<Class, AuditableMetadata> cache = new HashMap<Class, AuditableMetadata>();

	private Class<?> clazz;
	private Field createdByField;
	private Field createdDateField;
	private Field lastModifiedByField;
	private Field lastModifiedDateField;

	protected AuditableMetadata(Class<?> clazz) {
		this.clazz = clazz;
		discoverAuditProperties();
	}

	/*
	 * (non-Javadoc)
	 * Discover fields annotated by {@link CreatedBy}, {@link CreatedDate}, {@link LastModifiedBy},
	 * {@link LastModifiedDate}.
	 */
	private void discoverAuditProperties() {
		ReflectionUtils.doWithFields(clazz, new CreatedByFieldCallback());
		ReflectionUtils.doWithFields(clazz, new CreatedDateFieldCallback());
		ReflectionUtils.doWithFields(clazz, new LastModifiedByFieldCallback());
		ReflectionUtils.doWithFields(clazz, new LastModifiedDateFieldCallback());
	}

	/**
	 * Return a {@link AuditableMetadata} for the given {@link Class}. The cache is first checked for an existing
	 * {@link AuditableMetadata} otherwise a new one is created.
	 */
	public static AuditableMetadata getMetadata(Class<?> clazz) {
		if (cache.containsKey(clazz)) {
			return cache.get(clazz);
		}
		AuditableMetadata metadata = new AuditableMetadata(clazz);
		cache.put(clazz, metadata);
		return metadata;
	}

	/**
	 * Returns if the {@link Class} represented in this instance is auditable or not.
	 */
	public boolean isAuditable() {
		if ((createdByField == null) && (createdDateField == null) && (lastModifiedByField == null)
				&& (lastModifiedDateField == null)) {
			return false;
		}
		return true;
	}

	/**
	 * Return the field annotated by {@link CreatedBy}, or null
	 */
	public Field getCreatedByField() {
		return createdByField;
	}

	/**
	 * Return the field annotated by {@link CreatedDate}, or null
	 */
	public Field getCreatedDateField() {
		return createdDateField;
	}

	/**
	 * Return the field annotated by {@link LastModifiedBy}, or null
	 */
	public Field getLastModifiedByField() {
		return lastModifiedByField;
	}

	/**
	 * Return the field annotated by {@link LastModifiedDate}, or null
	 */
	public Field getLastModifiedDateField() {
		return lastModifiedDateField;
	}

	/**
	 * Return the field annotated by {@link CreatedBy}, or null
	 */
	private final class CreatedByFieldCallback implements ReflectionUtils.FieldCallback {
		public void doWith(Field field) {
			if (field.getAnnotation(CreatedBy.class) != null) {
				createdByField = field;
			}
		}
	}

	private final class CreatedDateFieldCallback implements ReflectionUtils.FieldCallback {
		public void doWith(Field field) {
			if (field.getAnnotation(CreatedDate.class) != null) {
				createdDateField = field;
			}
		}
	}

	private final class LastModifiedByFieldCallback implements ReflectionUtils.FieldCallback {
		public void doWith(Field field) {
			if (field.getAnnotation(LastModifiedBy.class) != null) {
				lastModifiedByField = field;
			}
		}
	}

	private final class LastModifiedDateFieldCallback implements ReflectionUtils.FieldCallback {
		public void doWith(Field field) {
			if (field.getAnnotation(LastModifiedDate.class) != null) {
				lastModifiedDateField = field;
			}
		}
	}
}
