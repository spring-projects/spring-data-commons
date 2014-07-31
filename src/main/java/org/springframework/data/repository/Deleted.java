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

/**
 * Enum based implementation of {@link DeleteStates}.
 * 
 * @since 1.9
 * @author Oliver Gierke
 */
public enum Deleted implements DeleteStates<Deleted> {

	/**
	 * The object is alive an considered accessable.
	 */
	ALIVE {
		@Override
		public Deleted delete() {
			return TRASH;
		}
	},

	/**
	 * The object is considered trashed but not deleted eventually.
	 */
	TRASH,

	/**
	 * The object is deleted.
	 */
	DELETED {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.Deleted#restore()
		 */
		@Override
		public Deleted restore() {
			return TRASH;
		}
	};

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.DeleteStates#activeValue()
	 */
	public Deleted activeValue() {
		return ALIVE;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.DeleteStates#delete()
	 */
	public Deleted delete() {
		return DELETED;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.DeleteStates#restore()
	 */
	public Deleted restore() {
		return ALIVE;
	}
}
