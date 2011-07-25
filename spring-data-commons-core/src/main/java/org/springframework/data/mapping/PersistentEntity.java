package org.springframework.data.mapping;

import org.springframework.data.util.TypeInformation;

/**
 * Represents a persistent entity
 * 
 * @author Graeme Rocher
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public interface PersistentEntity<T, P extends PersistentProperty<P>> {

	/**
	 * The entity name including any package prefix
	 * 
	 * @return must never return {@literal null}
	 */
	String getName();

	/**
	 * Returns the {@link PreferredConstructor} to be used to instantiate objects of this {@link PersistentEntity}.
	 * 
	 * @return {@literal null} in case no suitable constructor for automatic construction can be found.
	 */
	PreferredConstructor<T> getPreferredConstructor();

	/**
	 * Returns the id property of the {@link PersistentEntity}. Must never return {@literal null} as a
	 * {@link PersistentEntity} instance must not be created if there is no id property.
	 * 
	 * @return the id property of the {@link PersistentEntity}.
	 */
	P getIdProperty();

	/**
	 * Obtains a PersistentProperty instance by name.
	 * 
	 * @param name The name of the property
	 * @return The {@link PersistentProperty} or {@literal null} if it doesn't exist
	 */
	P getPersistentProperty(String name);

	/**
	 * Returns the resolved Java type of this entity.
	 * 
	 * @return The underlying Java class for this entity
	 */
	Class<T> getType();

	/**
	 * Returns the {@link TypeInformation} backing this {@link PersistentEntity}.
	 * 
	 * @return
	 */
	TypeInformation<T> getTypeInformation();

	/**
	 * Applies the given {@link PropertyHandler} to all {@link PersistentProperty}s contained in this
	 * {@link PersistentEntity}.
	 * 
	 * @param handler must not be {@literal null}.
	 */
	void doWithProperties(PropertyHandler<P> handler);

	/**
	 * Applies the given {@link AssociationHandler} to all {@link Association} contained in this {@link PersistentEntity}.
	 * 
	 * @param handler must not be {@literal null}.
	 */
	void doWithAssociations(AssociationHandler<P> handler);
}
