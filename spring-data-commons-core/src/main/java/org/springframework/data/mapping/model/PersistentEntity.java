package org.springframework.data.mapping.model;

import java.util.Collection;

import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
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
	 * @return The entity name
	 */
	String getName();

	PreferredConstructor<T> getPreferredConstructor();


	/**
	 * Returns the id property of the {@link PersistentEntity}. Must never
	 * return {@literal null} as a {@link PersistentEntity} instance must not be
	 * created if there is no id property.
	 *
	 * @return the id property of the {@link PersistentEntity}.
	 */
	P getIdProperty();

	/**
	 * A list of properties to be persisted
	 *
	 * @return A list of PersistentProperty instances
	 */
	Collection<P> getPersistentProperties();

	/**
	 * A list of the associations for this entity. This is typically a subset of the list returned by {@link #getPersistentProperties()}
	 *
	 * @return A list of associations
	 */
	Collection<Association<P>> getAssociations();

	/**
	 * Obtains a PersistentProperty instance by name
	 *
	 * @param name The name of the property
	 * @return The PersistentProperty or null if it doesn't exist
	 */
	P getPersistentProperty(String name);

	/**
	 * @return The underlying Java class for this entity
	 */
	Class<T> getType();

	TypeInformation<T> getTypeInformation();

	/**
	 * A list of property names
	 *
	 * @return A List of strings
	 */
	Collection<String> getPersistentPropertyNames();

	void doWithProperties(PropertyHandler<P> handler);

	void doWithAssociations(AssociationHandler<P> handler);
}
