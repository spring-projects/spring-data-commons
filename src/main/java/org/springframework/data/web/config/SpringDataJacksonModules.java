/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.data.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Marker interface to describe configuration classes that ship Jackson modules that are supposed to be added to the
 * Jackson {@link ObjectMapper} configured for {@link EnableSpringDataWebSupport}.
 *
 * @author Oliver Gierke
 * @since 1.13
 */
public interface SpringDataJacksonModules {}
