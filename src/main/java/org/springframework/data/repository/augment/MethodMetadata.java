/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.data.repository.augment;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

/**
 * Interface to abstract {@link MethodMetadata} to be looked up for the repository method invoked.
 * 
 * @see 1.11
 * @author Oliver Gierke
 */
public interface MethodMetadata {

	/**
	 * Returns the arguments piped into the current repository method invocation.
	 * 
	 * @return
	 */
	Object[] getInvocationArguments();

	/**
	 * Returns the type about to be invoked. This will usually be the type of the repository invoked, even if the method
	 * is actually declared in a Spring Data repository interface (e.g. {@link CrudRepository}).
	 * 
	 * @return
	 */
	List<Class<?>> getInvocationTargetType();

	/**
	 * Returns the method invoked at the repository level.
	 * 
	 * @return
	 */
	Method getMethod();
}
