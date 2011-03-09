package org.springframework.persistence.support;

import java.lang.reflect.Field;

import javax.validation.constraints.NotNull;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.persistence.AsynchStoreCompletionListener;
import org.springframework.persistence.InvalidFieldAnnotationException;
import org.springframework.persistence.MappingValidator;
import org.springframework.persistence.RelatedEntity;

// TODO fancier version could discover many rules, with annotations etc.
// Also invoke the relevant EntityOperations
public class SimpleMappingValidator implements MappingValidator {

	@Override
	public void validateGet(Class<?> entityClass, Field f, RelatedEntity re) throws InvalidDataAccessApiUsageException {
		// Validate the annotation
		if (!AsynchStoreCompletionListener.NONE.class.equals(re.storeCompletionListenerClass())
				&& !"".equals(re.storeCompletionListenerBeanName())) {
			throw new InvalidFieldAnnotationException(entityClass, f,
					"Can't have storeCompletionListener class and bean name on same annotation");
		}
	}

	public void validateSetTo(Class<?> entityClass, Field f, RelatedEntity re, Object newVal) throws InvalidDataAccessApiUsageException,
			IllegalArgumentException {
		if (newVal == null && f.isAnnotationPresent(NotNull.class)) {
			throw new IllegalArgumentException("Can't set non-null field [" + f.getName() + " to null");
		}
	}

}
