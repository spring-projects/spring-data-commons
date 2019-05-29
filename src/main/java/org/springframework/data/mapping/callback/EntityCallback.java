/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.mapping.callback;

/**
 * Marker interface for entity callbacks to be implemented in specific callback subtypes intended for internal usage
 * within store specific implementations. <br />
 * <p />
 * Multiple entity callbacks are invoked sequentially with the result of the previous callback. Those callbacks do by
 * default not follow an explicit order of invocation. It is strongly recommended to enforce ordering for callbacks of
 * the same by implementing {@link org.springframework.core.Ordered} or following the annotation driven approach using
 * {@link org.springframework.core.annotation.Order}.
 * <p />
 * Entity callbacks are invoked after publishing {@link org.springframework.context.ApplicationEvent events}.
 * <p />
 * A store specific {@link EntityCallback} needs to define a callback method accepting an object of the parameterized
 * type as its first argument followed by additional <i>optional</i> arguments.
 * 
 * <pre>
 * <code>
 *
 * public interface BeforeSaveCallback&lt;T&gt; extends EntityCallback&lt;T&gt; {
 *
 *     T onBeforeSave(T entity, String collection);
 * }
 * </code>
 * </pre>
 *
 * The
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @param <T> Entity type. Used to detect {@link EntityCallback callbacks} to invoke via their generic type signature.
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 */
public interface EntityCallback<T> {

}
