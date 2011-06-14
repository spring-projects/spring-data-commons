/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Simple value object to capture information of {@link PersistentEntity}s.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BasicPersistentEntity<T, P extends PersistentProperty<P>> implements MutablePersistentEntity<T, P> {

	protected final PreferredConstructor<T> preferredConstructor;
	protected final TypeInformation<T> information;
	protected final Map<String, P> persistentProperties = new HashMap<String, P>();
	protected final Map<String, Association<P>> associations = new HashMap<String, Association<P>>();
	protected P idProperty;


	/**
	 * Creates a new {@link BasicPersistentEntity} from the given {@link TypeInformation}.
	 *
	 * @param information
	 */
	public BasicPersistentEntity(TypeInformation<T> information) {
		Assert.notNull(information);
		this.information = information;
		this.preferredConstructor = new PreferredConstructorDiscoverer<T>(information).getConstructor();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPreferredConstructor()
	 */
	public PreferredConstructor<T> getPreferredConstructor() {
		return preferredConstructor;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getName()
	 */
	public String getName() {
		return getType().getName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getIdProperty()
	 */
	public P getIdProperty() {
		return idProperty;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#setIdProperty(P)
	 */
	public void setIdProperty(P property) {
		idProperty = property;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#addPersistentProperty(P)
	 */
	public void addPersistentProperty(P property) {
		Assert.notNull(property);
		persistentProperties.put(property.getName(), property);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#addAssociation(org.springframework.data.mapping.model.Association)
	 */
	public void addAssociation(Association<P> association) {
		associations.put(association.getInverse().getName(), association);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistentProperty(java.lang.String)
	 */
	public P getPersistentProperty(String name) {
		return persistentProperties.get(name);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getType()
	 */
	public Class<T> getType() {
		return information.getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getTypeInformation()
	 */
	public TypeInformation<T> getTypeInformation() {
		return information;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistentPropertyNames()
	 */
	public Collection<String> getPersistentPropertyNames() {
		return persistentProperties.keySet();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithProperties(org.springframework.data.mapping.PropertyHandler)
	 */
	public void doWithProperties(PropertyHandler<P> handler) {
		Assert.notNull(handler);
		for (P property : persistentProperties.values()) {
			if (!property.isTransient() && !property.isAssociation()) {
				handler.doWithPersistentProperty(property);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithAssociations(org.springframework.data.mapping.AssociationHandler)
	 */
	public void doWithAssociations(AssociationHandler<P> handler) {
		Assert.notNull(handler);
		for (Association<P> association : associations.values()) {
			handler.doWithAssociation(association);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#verify()
	 */
	public void verify() {

	}
}
