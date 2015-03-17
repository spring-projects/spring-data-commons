/*
 * Copyright 2015 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport.SpringDataWebConfigurationImportSelector;

/**
 * Annotation to be able to scan for additional Spring Data configuration classes to contribute to the web integration.
 *
 * @author Oliver Gierke
 * @since 1.10
 * @soundtrack Selah Sue - This World (Selah Sue)
 * @see SpringDataJacksonConfiguration
 * @see SpringDataWebConfigurationImportSelector
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
public @interface SpringDataWebConfigurationMixin {
}
