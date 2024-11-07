/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.annotation;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be transient for the mapping framework. Thus, the property will not be persisted.
 * <p>
 * Excluding properties from the persistence mechanism is separate from Java's {@code transient} keyword that serves the
 * purpose of excluding properties from being serialized through Java Serialization.
 * <p>
 * Transient properties can be used in {@link PersistenceCreator constructor creation/factory methods}, however they
 * will use Java default values. We highly recommend using {@link org.springframework.beans.factory.annotation.Value
 * SpEL expressions through @Value(…)} to provide a meaningful value.
 *
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @author Mark Paluch
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { FIELD, METHOD, ANNOTATION_TYPE, RECORD_COMPONENT })
public @interface Transient {
}
