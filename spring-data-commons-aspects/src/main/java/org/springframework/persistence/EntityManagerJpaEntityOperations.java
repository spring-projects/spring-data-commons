package org.springframework.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.dao.DataAccessException;

/**
 * Implementation of entity operations that works on any entity
 * that adheres to Roo persist() and static finder conventions.
 * Does not depend on Roo, merely on Roo conventions, which can
 * also be implemented by hand.
 * 
 * @author Rod Johnson
 */
public class EntityManagerJpaEntityOperations extends OrderedEntityOperations {

	@PersistenceContext
	private EntityManager entityManager;
	
	public static Object invoke(Class<?> clazz, String methodName,
			Object target, Class<?>[] argTypes, Object... args) {
		try {
			Method m = clazz.getMethod(methodName, argTypes);
			return m.invoke(target, (Object[]) args);
		} catch (Exception ex) {
			// TODO FIX ME
			// System.out.println(ex + ": checked exceptions are stupid");
			throw new IllegalArgumentException(ex);
		}
	}

	public static Object invokeNoArgMethod(Class<?> clazz, String methodName, Object target) {
		return invoke(clazz, methodName, target, (Class<?>[]) null,
				(Object[]) null);
	}
	
	@Override
	public Object findEntity(Class entityClass, Object pk)
			throws DataAccessException {
		String findMethod = "find" + entityClass.getSimpleName();
		Object found =  entityManager.find(entityClass, pk);
		log.info("Lookup [" + entityClass.getName() + "] by pk=[" + pk + "] using EntityManager.find() - found [" + found + "]");
		return found;
	}


	@Override
	public Object findUniqueKey(Object entity) throws DataAccessException {
		String idMethodName = "getId";
		return invokeNoArgMethod(entity.getClass(), idMethodName, entity);
	}

	@Override
	public boolean isTransient(Object entity) throws DataAccessException {
		return findUniqueKey(entity) == null;
	}

	@Override
	public Object makePersistent(Object owner, Object entity, Field f, RelatedEntity fs) throws DataAccessException {
		if (log.isDebugEnabled()) {
			log.debug("Making entity persistent: BEFORE [" + entity + "]");
		}
		entityManager.persist(entity);
		Object key = findUniqueKey(entity);
		log.info("Making entity persistent: AFTER [" + entity + "]");
		return key;
	}

	@Override
	public boolean supports(Class entityClass, RelatedEntity fs) {
		return entityClass.isAnnotationPresent(Entity.class);
	}
	
	@Override
	public boolean cacheInEntity() {
		return false;
	}

	@Override
	public boolean isTransactional() {
		// TODO Need to have a better test
		return true;
	}}
