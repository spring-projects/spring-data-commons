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

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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
 * @author Oliver Gierke
 */
public class BasicPersistentEntity<T, P extends PersistentProperty<P>> implements MutablePersistentEntity<T, P> {

	private final PreferredConstructor<T> preferredConstructor;
	private final TypeInformation<T> information;
	private final Set<P> properties;
	private final Set<Association<P>> associations;

	private P idProperty;

	/**
	 * Creates a new {@link BasicPersistentEntity} from the given {@link TypeInformation}.
	 * 
	 * @param information must not be {@literal null}.
	 */
	public BasicPersistentEntity(TypeInformation<T> information) {
		this(information, null);
	}

	/**
	 * Creates a new {@link BasicPersistentEntity} for the given {@link TypeInformation} and {@link Comparator}. The given
	 * {@link Comparator} will be used to define the order of the {@link PersistentProperty} instances added to the
	 * entity.
	 * 
	 * @param information must not be {@literal null}
	 * @param comparator
	 */
	public BasicPersistentEntity(TypeInformation<T> information, Comparator<P> comparator) {
		Assert.notNull(information);
		this.information = information;
		this.preferredConstructor = new PreferredConstructorDiscoverer<T>(information).getConstructor();
		this.properties = comparator == null ? new HashSet<P>() : new TreeSet<P>(comparator);
		this.associations = comparator == null ? new HashSet<Association<P>>() : new TreeSet<Association<P>>(
				new AssociationComparator<P>(comparator));
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
		properties.add(property);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#addAssociation(org.springframework.data.mapping.model.Association)
	 */
	public void addAssociation(Association<P> association) {
		associations.add(association);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistentProperty(java.lang.String)
	 */
	public P getPersistentProperty(String name) {

		for (P property : properties) {
			if (property.getName().equals(name)) {
				return property;
			}
		}

		return null;
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
	 * @see org.springframework.data.mapping.PersistentEntity#doWithProperties(org.springframework.data.mapping.PropertyHandler)
	 */
	public void doWithProperties(PropertyHandler<P> handler) {
		Assert.notNull(handler);
		for (P property : properties) {
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
		for (Association<P> association : associations) {
			handler.doWithAssociation(association);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#verify()
	 */
	public void verify() {

	}

	/**
	 * Simple {@link Comparator} adaptor to delegate ordering to the inverse properties of the association.
	 * 
	 * @author Oliver Gierke
	 */
	private static final class AssociationComparator<P extends PersistentProperty<P>> implements
			Comparator<Association<P>>, Serializable {

		private static final long serialVersionUID = 4508054194886854513L;
		private final Comparator<P> delegate;

		public AssociationComparator(Comparator<P> delegate) {
			Assert.notNull(delegate);
			this.delegate = delegate;
		}

		public int compare(Association<P> left, Association<P> right) {
			return delegate.compare(left.getInverse(), right.getInverse());
		}
	}
}
