/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.annotation;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker interface for methods with {@link Persistent} annotations indicating the presence of a dedicated keyspace the
 * entity should reside in. If present the value will be picked up for resolving the keyspace.
 * 
 * <pre>
 * <code>
 * &#64;Persistent
 * &#64;Documented
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * &#64;Target({ ElementType.TYPE })
 * public &#64;interface Document {
 * 
 * 		&#64;KeySpace
 * 		String collection() default "person";
 * } 
 * </code>
 * </pre>
 * 
 * Can also be directly used on types to indicate the keyspace.
 * 
 * <pre>
 * <code>
 * &#64;KeySpace("persons")
 * public class Foo {
 * 
 * } 
 * </code>
 * </pre>
 * 
 * @author Christoph Strobl
 * @since 1.10
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { METHOD, TYPE })
public @interface KeySpace {

	String value() default "";
}
