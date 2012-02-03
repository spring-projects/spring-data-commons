package org.springframework.data.mapping;

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

	/**
	 * Returns the {@link TypeInformation} if the property references a {@link PersistentEntity}. Will return
	 * {@literal null} in case it refers to a simple type. Will return {@link Collection}'s component type or the
	 * {@link Map}'s value type transparently.
	 * 
	 * @return
	 */
	Iterable<? extends TypeInformation<?>> getPersistentEntityType();

	/**
	 * Returns the {@link PropertyDescriptor} backing the {@link PersistentProperty}.
	 * 
	 * @return
	 */
	PropertyDescriptor getPropertyDescriptor();

	Field getField();

	String getSpelExpression();

	Association<P> getAssociation();

	/**
	 * Returns whether the property is the ID property of the owning {@link PersistentEntity}.
	 * 
	 * @return
	 */
	boolean isIdProperty();

	/**
	 * Returns whether the property is a {@link Collection}, {@link Iterable} or an array.
	 * 
	 * @return
	 */
	boolean isCollectionLike();

	/**
	 * Returns whether the property is a {@link Map}.
	 * 
	 * @return
	 */
	boolean isMap();

	/**
	 * Returns whether the property is an array.
	 * 
	 * @return
	 */
	boolean isArray();

	/**
	 * Returns whether the property is transient.
	 * 
	 * @return
	 */
	boolean isTransient();

	boolean shallBePersisted();

	/**
	 * Returns whether the property is an {@link Association}.
	 * 
	 * @return
	 */
	boolean isAssociation();

	/**
	 * Returns the component type of the type if it is a {@link java.util.Collection}. Will return the type of the key if
	 * the property is a {@link java.util.Map}.
	 * 
	 * @return the component type, the map's key type or {@literal null} if neither {@link java.util.Collection} nor
	 *         {@link java.util.Map}.
	 */
	Class<?> getComponentType();

	/**
	 * Returns the raw type as it's pulled from from the reflected property.
	 * 
	 * @return the raw type of the property.
	 */
	Class<?> getRawType();

	/**
	 * Returns the type of the values if the property is a {@link java.util.Map}.
	 * 
	 * @return the map's value type or {@literal null} if no {@link java.util.Map}
	 */
	Class<?> getMapValueType();

}
