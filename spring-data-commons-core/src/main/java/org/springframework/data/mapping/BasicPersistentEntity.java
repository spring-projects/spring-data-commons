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
package org.springframework.data.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.model.Association;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.mapping.model.PreferredConstructor;
import org.springframework.data.util.TypeInformation;

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
		this.information = information;
		this.preferredConstructor = new PreferredConstructorDiscoverer<T>(information).getConstructor();
	}

	public PreferredConstructor<T> getPreferredConstructor() {
		return preferredConstructor;
	}

	public String getName() {
		return getType().getName();
	}

	public P getIdProperty() {
		return idProperty;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#setIdProperty(P)
	 */
	public void setIdProperty(P property) {
		idProperty = property;
	}

	public Collection<P> getPersistentProperties() {
		return persistentProperties.values();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#addPersistentProperty(P)
	 */
	public void addPersistentProperty(P property) {
		persistentProperties.put(property.getName(), property);
	}

	public Collection<Association<P>> getAssociations() {
		return associations.values();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#addAssociation(org.springframework.data.mapping.model.Association)
	 */
	public void addAssociation(Association<P> association) {
		associations.put(association.getInverse().getName(), association);
	}

	public P getPersistentProperty(String name) {
		return persistentProperties.get(name);
	}

	public Class<T> getType() {
		return information.getType();
	}

	public TypeInformation<T> getTypeInformation() {
		return information;
	}

	public Collection<String> getPersistentPropertyNames() {
		return persistentProperties.keySet();
	}

	public void doWithProperties(PropertyHandler<P> handler) {
		for (P property : persistentProperties.values()) {
			if (!property.isTransient() && !property.isAssociation()) {
				handler.doWithPersistentProperty(property);
			}
		}
	}

	public void doWithAssociations(AssociationHandler<P> handler) {
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
