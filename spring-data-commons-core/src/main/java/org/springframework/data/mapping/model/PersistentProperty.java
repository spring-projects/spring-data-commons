package org.springframework.data.mapping.model;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import org.springframework.data.util.TypeInformation;

/**
 * @author Graeme Rocher
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public interface PersistentProperty<P extends PersistentProperty<P>> {

	PersistentEntity<?, P> getOwner();

	/**
	 * The name of the property
	 *
	 * @return The property name
	 */
	String getName();

	/**
	 * The type of the property
	 *
	 * @return The property type
	 */
	Class<?> getType();

	TypeInformation<?> getTypeInformation();

	PropertyDescriptor getPropertyDescriptor();

	Field getField();

	String getSpelExpression();

	boolean isTransient();

	boolean isAssociation();

	Association<P> getAssociation();

	boolean isCollection();

	boolean isMap();

	boolean isArray();

	boolean isComplexType();

	/**
	 * Returns whether the property has to be regarded as entity which means its type will be also be considered to be a
	 * {@link PersistentEntity}.
	 *
	 * @return
	 */
	boolean isEntity();

	/**
	 * Returns the component type of the type if it is a {@link Collection}. Will return the type of the key if the
	 * property is a {@link Map}.
	 *
	 * @return the component type, the map's key type or {@literal null} if neither {@link Collection} nor {@link Map}.
	 */
	Class<?> getComponentType();

	/**
	 * Returns the raw type as it's pulled from from the reflected property.
	 *
	 * @return the raw type of the property.
	 */
	Class<?> getRawType();

	/**
	 * Returns the type of the values if the property is a {@link Map}.
	 *
	 * @return the map's value type or {@literal null} if no {@link Map}
	 */
	Class<?> getMapValueType();

	boolean isIdProperty();
}
