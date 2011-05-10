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

package org.springframework.data.mapping.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.util.TypeInformation;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class MappingContextEvent<E extends PersistentEntity<?, P>, P extends PersistentProperty<P>> extends ApplicationEvent {

	private static final long serialVersionUID = 1336466833846092490L;
	private TypeInformation<?> typeInformation;

	public MappingContextEvent(E source, TypeInformation<?> typeInformation) {
		super(source);
		this.typeInformation = typeInformation;
	}

	public TypeInformation<?> getTypeInformation() {
		return typeInformation;
	}

	@SuppressWarnings("unchecked")
	public E getPersistentEntity() {
		return (E) source;
	}
}
