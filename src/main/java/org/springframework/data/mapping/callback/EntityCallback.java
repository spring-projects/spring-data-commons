/*
 * Copyright 2019-2021 the original author or authors.
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
 * Marker interface for entity callbacks to be implemented in specific callback subtypes. Intended for internal usage
 * within store specific implementations.
 * <h3>Ordering {@link EntityCallback}</h3>
 * <p/>
 * Multiple entity callbacks are invoked sequentially with the result of the previous callback. Callbacks are unordered
 * by default. It is possible to define the order in which listeners for a certain domain type are to be invoked. To do
 * so, add Spring's common {@link org.springframework.core.annotation.Order @Order} annotation or implement
 * {@link org.springframework.core.Ordered}.
 * <h3>Exception Handling</h3>
 * <p />
 * While it is possible for a {@link EntityCallback} to declare that it throws arbitrary exception types, any checked
 * exceptions thrown from a {@link EntityCallback} are wrapped in an
 * {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException} since the callback mechanism can
 * only handle runtime exceptions. Entity callback processing is stopped on the {@link EntityCallback} that raised an
 * exception and the caused exception is propagated to the caller.
 * <h3>Domain Type Binding</h3>
 * <p />
 * An {@link EntityCallback} can generically declare the domain type that it is able to process by specifying the
 * generic type parameter {@code <T>}. When registered with a Spring
 * {@link org.springframework.context.ApplicationContext}, callbacks are filtered accordingly, with the callback getting
 * invoked for assignable domain objects only.
 * <p/>
 * Typically, entity callbacks are invoked after publishing {@link org.springframework.context.ApplicationEvent events}.
 * <p/>
 * <h3>Defining {@link EntityCallback} Interfaces</h3>
 * <p />
 * A {@link EntityCallback} interface needs to define a callback method accepting an object of the parameterized type as
 * its first argument followed by additional <i>optional</i> arguments.
 *
 * <pre class="code">
 *
 * public interface BeforeSaveCallback&lt;T&gt; extends EntityCallback&lt;T&gt; {
 *
 * 	T onBeforeSave(T entity, String collection);
 * }
 * </pre>
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @param <T> Entity type used to detect {@link EntityCallback callbacks} to invoke via their generic type signature.
 * @since 2.2
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 */
public interface EntityCallback<T> {

}
