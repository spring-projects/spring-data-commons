/*
 * Copyright 2011-2021 the original author or authors.
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

import org.springframework.core.annotation.AliasFor;

/**
 * Meta-annotation to be used to annotate annotations that mark references to other objects.
 *
 * @author Oliver Gierke
 * @author Jon Brisbin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { FIELD, METHOD, ANNOTATION_TYPE })
public @interface Reference {

	/**
	 * Explicitly define the target type of the reference. Used in case the annotated property is not the target type but
	 * rather an identifier and/or if that identifier type is not uniquely identifying the target entity.
	 *
	 * @return
	 */
	@AliasFor(attribute = "to")
	Class<?> value() default Class.class;

	/**
	 * Explicitly define the target type of the reference. Used in case the annotated property is not the target type but
	 * rather an identifier and/or if that identifier type is not uniquely identifying the target entity.
	 *
	 * @return
	 */
	@AliasFor(attribute = "value")
	Class<?> to() default Class.class;
}
