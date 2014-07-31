/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable soft-delete handling for entities of a given repository.
 * 
 * @since 1.9
 * @author Oliver Gierke
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
public @interface SoftDelete {

	/**
	 * The name of the property that holds the deleted state.
	 * 
	 * @return
	 */
	String value();

	/**
	 * The mode of the property representing the deleted state. By default we assume a boolean flag being set to
	 * {@literal true} in case the entity shall be considered deleted.
	 * 
	 * @return
	 */
	FlagMode flagMode() default FlagMode.DELETED;

	/**
	 * Various strategies of how to interpret the entity's property capturing the deleted state.
	 * 
	 * @since 1.9
	 * @author Oliver Gierke
	 */
	public enum FlagMode {

		/**
		 * The flag in the domain type represents the active state. Thus, {@literal true} means it's active,
		 * {@literal false} is considered inactive or deleted. The opposite of {@link FlagMode#DELETED}. Expects the object
		 * property to be of type {@link Boolean} or {@literal boolean}.
		 */
		ACTIVE,

		/**
		 * The flag in the domain type represents the deleted state. Thus {@literal true} means it's deleted,
		 * {@literal false} means the object is active. The opposite of {@link FlagMode#ACTIVE}. Expects the object property
		 * to be of type {@link Boolean} or {@literal boolean}.
		 */
		DELETED {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.repository.SoftDelete.FlagMode#toDeletedValue(java.lang.Object)
			 */
			@Override
			public Object toDeletedValue(Object currentValue) {
				return true;
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.repository.SoftDelete.FlagMode#activeValue()
			 */
			@Override
			public Object activeValue() {
				return false;
			}
		},

		/**
		 * This strategy expects the property type of the value expresing the deleted state to implement
		 * {@link DeleteStates} and delegate to {@link DeleteStates#delete()} to determine the next value to be set when
		 * attempting a delete. The easiest way is to use {@link Deleted} but you can essentially implement any custom type
		 * that follows the spec defined in {@link DeleteStates}.
		 * 
		 * @see Deleted
		 */
		TRASHABLE {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.repository.SoftDelete.FlagMode#toDeletedValue(java.lang.Object)
			 */
			@Override
			public Object toDeletedValue(Object currentValue) {

				if (currentValue == null) {
					return null;
				}

				if (!(currentValue instanceof Deleted)) {
					throw new IllegalArgumentException("Trashable flag mode only supports values of type Deleted!");
				}

				return ((Deleted) currentValue).delete();
			}
		};

		/**
		 * Returns the value that represents that the entity is active.
		 * 
		 * @return
		 */
		public Object activeValue() {
			return true;
		}

		/**
		 * Returns the value to be used to express the entity being deleted. This can happen over a variety of stages, thus
		 * we need the current value to potentially determine the next.
		 * 
		 * @param currentValue can be {@literal null}.
		 * @return
		 */
		public Object toDeletedValue(Object currentValue) {
			return false;
		}
	}
}
