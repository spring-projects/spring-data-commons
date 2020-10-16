/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import java.lang.annotation.Annotation;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;

/**
 * @author Christoph Strobl
 * @since 2020/10
 */
public interface AnnotationAware {

	Id ID_ANNOTATION = new Id() {
		@Override
		public Class<? extends Annotation> annotationType() {
			return Id.class;
		}
	};

	Persistent PERSISTENT_ANNOTATION = new Persistent() {
		@Override
		public Class<? extends Annotation> annotationType() {
			return Persistent.class;
		}
	};

	Transient TRANSIENT_ANNOTATION = new Transient() {

		@Override
		public Class<? extends Annotation> annotationType() {
			return Transient.class;
		}
	};

	Immutable IMMUTABLE_ANNOTATION = new Immutable() {
		@Override
		public Class<? extends Annotation> annotationType() {
			return Immutable.class;
		}
	};

	List<Annotation> getAnnotations();

	boolean hasAnnotation(Class<?> annotationType);

	<T extends Annotation> List<T> findAnnotation(Class<T> annotation);

	static Id idAnnotation() {
		return ID_ANNOTATION;
	}

	static Persistent persistentAnnotation() {
		return PERSISTENT_ANNOTATION;
	}

	static Transient transientAnnotation() {
		return TRANSIENT_ANNOTATION;
	}

	static Immutable immutableAnnotation() {
		return IMMUTABLE_ANNOTATION;
	}

	static TypeAlias typeAliasAnnotation(String alias) {

		return new TypeAlias() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return TypeAlias.class;
			}

			@Override
			public String value() {
				return alias;
			}
		};
	}

}
