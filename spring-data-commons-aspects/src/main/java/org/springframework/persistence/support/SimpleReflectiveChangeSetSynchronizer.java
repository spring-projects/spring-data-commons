package org.springframework.persistence.support;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.persistence.RelatedEntity;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;

/**
 * Synchronizes fields to ChangeSets, regardless of visibility.
 * 
 * @author Rod Johnson
 */
public class SimpleReflectiveChangeSetSynchronizer implements ChangeSetSynchronizer<ChangeSetBacked> {
			
	/**
	 * Filter matching infrastructure fields, so they can be excluded
	 */
	private static FieldFilter PERSISTABLE_FIELDS = new FieldFilter() {
		@Override
		public boolean matches(Field f) {
			return !(
					f.isSynthetic() || 
					Modifier.isStatic(f.getModifiers()) ||
					Modifier.isTransient(f.getModifiers()) ||
					f.getName().startsWith("ajc$") ||
					f.isAnnotationPresent(RelatedEntity.class)
			);
		}
	};
	
	private final Log log = LogFactory.getLog(getClass());

	private final ConversionService conversionService;
	
	@Autowired
	public SimpleReflectiveChangeSetSynchronizer(ConversionService conversionService) {
		this.conversionService = conversionService;
	}
	
	@Override
	public Map<String, Class<?>> persistentFields(Class<? extends ChangeSetBacked> entityClass) {
		final Map<String, Class<?>> fields = new HashMap<String, Class<?>>();
		ReflectionUtils.doWithFields(entityClass, new FieldCallback() {
			@Override
			public void doWith(Field f) throws IllegalArgumentException, IllegalAccessException {
				fields.put(f.getName(), f.getType());
			}
		}, PERSISTABLE_FIELDS);
		return fields;
	}

	@Override
	public void populateChangeSet(final ChangeSet changeSet, final ChangeSetBacked entity) throws DataAccessException {
		ReflectionUtils.doWithFields(entity.getClass(), new FieldCallback() {
			@Override
			public void doWith(Field f) throws IllegalArgumentException, IllegalAccessException {
				f.setAccessible(true);
				if (log.isDebugEnabled()) {
					log.debug("POPULATE ChangeSet value from entity field: " + f);
				}
				changeSet.set(f.getName(), f.get(entity));
			}
		}, PERSISTABLE_FIELDS);
		String classShortName = ClassUtils.getShortName(entity.getClass());
		changeSet.set("_class", classShortName);
	}

	@Override
	public void populateEntity(final ChangeSet changeSet, final ChangeSetBacked entity) throws DataAccessException {
		ReflectionUtils.doWithFields(entity.getClass(), new FieldCallback() {
			@Override
			public void doWith(Field f) throws IllegalArgumentException, IllegalAccessException {
				if (changeSet.getValues().containsKey(f.getName())) {
					f.setAccessible(true);
					if (log.isDebugEnabled()) {
						log.debug("POPULATE entity from ChangeSet for field: " + f);
					}
					Object val = changeSet.get(f.getName(), f.getType(), conversionService);									
					f.set(entity, val);
				}
			}
		}, PERSISTABLE_FIELDS);
	}

}
