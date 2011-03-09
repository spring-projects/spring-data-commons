package org.springframework.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.reflect.FieldSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.persistence.support.DefaultManagedSet;
import org.springframework.persistence.support.ManagedSet;
import org.springframework.persistence.support.ManagedSet.ChangeListener;

/**
 * Aspect to handle ForeignStore annotation indicating navigation to a
 * potentially different persistence store.
 * 
 * Can be configured via invoking init() method or through Spring
 * autowiring if beans named "entityOperationsLocator" and
 * "foreignStoreKeyManager" are provided.
 * 
 * @author Rod Johnson
 */
public privileged aspect StoreSpanning {

	private final Log log = LogFactory.getLog(getClass());

	private EntityOperationsLocator entityOperationsLocator;

	private ForeignStoreKeyManagerLocator foreignStoreKeyManagerLocator;
	
	private MappingValidator mappingValidator;

	@Autowired
	public void init(EntityOperationsLocator eol, ForeignStoreKeyManagerLocator fskml) {
		this.entityOperationsLocator = eol;
		this.foreignStoreKeyManagerLocator = fskml;
	}
	
	@Autowired(required=false)
	public void setMappingValidator(MappingValidator mv) {
		this.mappingValidator = mv;
	}
	

	public pointcut foreignEntityFieldGet(Object entity, RelatedEntity fs) : 
		get(@RelatedEntity * *) && 
		this(entity) &&
		@annotation(fs);

	public pointcut foreignEntityFieldSet(Object entity, RelatedEntity fs, Object newVal) : 
		set(@RelatedEntity * *) && 
		this(entity) &&
		@annotation(fs) &&
		args(newVal);

	@SuppressWarnings("unchecked")
	Object around(Object entity, RelatedEntity fs) : foreignEntityFieldGet(entity, fs) {
		Field f = ((FieldSignature) thisJoinPoint.getSignature()).getField();				
		log.info("GET: Handling foreign store " + f);
		if (this.mappingValidator != null) {
			this.mappingValidator.validateGet(entity.getClass(), f, fs);
		}
		
		Object fieldValue = proceed(entity, fs);
		// What if it was set to null?
		if (fieldValue != null) {
			log.info("GET " + f + ": returning actual field value");
			return fieldValue;
		}
		
		// Must retrieve
		if (Set.class.isAssignableFrom(f.getType())) {
			// TODO empty set, store class
			log.info("GET " + f + ":  Retrieving ManagedSet");
			ForeignStoreKeyManager foreignStoreKeyManager = foreignStoreKeyManagerLocator.foreignStoreKeyManagerFor(entity.getClass(), f);
			
			// TODO fix me, this is fragile
			ParameterizedType genericType = (ParameterizedType) f.getGenericType();
			Class entityClass = (Class) genericType.getActualTypeArguments()[0];
			Class keyClass = entityOperationsLocator.entityOperationsFor(entityClass, fs).uniqueKeyType(entityClass);
			Set keySet = foreignStoreKeyManager.findForeignStoreKeySet(entity, f, keyClass);
			ManagedSet managedSet = DefaultManagedSet.fromKeySet(keySet, entityClass, entityOperationsLocator);
			return managedSet;
		}
		else if (Collection.class.isAssignableFrom(f.getType())) {
			throw new UnsupportedOperationException("Unsupported collection type " + f.getType() + " in  entity class " + entity.getClass());
		}
		else {
			return findScalarEntity(entity, f, fs);
		}
	}
	
	private Object findScalarEntity(Object entity, Field f, RelatedEntity fs) {
		EntityOperations eo = entityOperationsLocator.entityOperationsFor(f.getType(), fs);
		Class keyType = eo.uniqueKeyType(f.getType());
		ForeignStoreKeyManager foreignStoreKeyManager = foreignStoreKeyManagerLocator.foreignStoreKeyManagerFor(entity.getClass(), f);
		Object pk = foreignStoreKeyManager.findForeignStoreKey(entity, f, keyType);
		if (pk != null) {
			log.debug("GET " + f + ": entity find for key=[" + pk + "] of class [" + pk.getClass() + "]");
			Object found = eo.findEntity(f.getType(), pk);
			log.info("GET " + f + ": entity find for key=[" + pk + "] found [" + found + "]");
			return found;
		}
		else {
			log.info("GET " + f + ": no key found, returning null");
			return null;
		}
	}

	// TODO handle explicit set to null
	@SuppressWarnings("unchecked")
	Object around(final Object entity, RelatedEntity fs, Object newVal) : foreignEntityFieldSet(entity, fs, newVal) {
		final Field f = ((FieldSignature) thisJoinPoint.getSignature()).getField();
		if (this.mappingValidator != null) {
			this.mappingValidator.validateSetTo(entity.getClass(), f, fs, newVal);
		}			
		log.info("SET: Handling foreign store " + f);
		
		if (newVal != null) {
			if (Set.class.isAssignableFrom(f.getType())) {
				log.info("Setting set: Creating ManagedSet");
				final ManagedSet managedSet = DefaultManagedSet.fromEntitySet((Set) newVal, entityOperationsLocator);
				final ForeignStoreKeyManager foreignStoreKeyManager = foreignStoreKeyManagerLocator.foreignStoreKeyManagerFor(entity.getClass(), f);
				foreignStoreKeyManager.storeForeignStoreKeySet(entity, f, managedSet.getKeySet());
				managedSet.addListener(new ChangeListener()  {					
					@Override
					public void onDirty() {
						foreignStoreKeyManager.storeForeignStoreKeySet(entity, f, managedSet.getKeySet());
					}
				});
				return proceed(entity, fs, managedSet);
			}
			else if (Collection.class.isAssignableFrom(f.getType())) {
				throw new UnsupportedOperationException("Unsupported collection type " + f.getType() + " in  entity class " + entity.getClass());
			}
			else {
				EntityOperations eo = handleScalarFieldSet(entity, f, fs, newVal);
	
				// Don't store it in the entity if the entity type doesn't support
				// it, for example
				// because it shouldn't be read repeatedly (as with a stream)
				if (!eo.cacheInEntity()) {
					return null;
				}
			}
		}
		return proceed(entity, fs, newVal);
	}
	
	private EntityOperations handleScalarFieldSet(Object entity, Field f, RelatedEntity fs, Object newVal) {
		EntityOperations eo = entityOperationsLocator.entityOperationsFor(f.getType(), fs);
		Object pk = eo.findUniqueKey(newVal);

		System.err.println("TODO: test whether current entity is persistent");
		if (pk == null) {
			// Entity is transient for now
			log.info("SET " + f + ": no foreign store key to store; entity has no persistent identity, MAKING PERSISTENT");
			pk = eo.makePersistent(entity,newVal, f, fs);
		}
		
		if (pk != null) {
			ForeignStoreKeyManager foreignStoreKeyManager = foreignStoreKeyManagerLocator.foreignStoreKeyManagerFor(entity.getClass(), f);
			foreignStoreKeyManager.storeForeignStoreKey(entity, f, pk);
			log.info("SET " + f + ": stored foreign store key=[" + pk + "]");
		}
		return eo;
	}
}
