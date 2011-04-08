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

package org.springframework.data.mapping.context;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.model.MappingContext;

/**
 * BeanPostProcessor to make Spring beans aware of the current MappingContext. If a MappingContext exists with the
 * default bean name ("mappingContext"), then that bean is used. If there is no MappingContext registered under the
 * default bean name, then the first MappingContext it finds is the one it chooses.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class MappingContextAwareBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

	private ApplicationContext applicationContext;
	private String mappingContextBeanName = "mappingContext";
	private MappingContext mappingContext;

	public MappingContextAwareBeanPostProcessor() {
	}

	public MappingContextAwareBeanPostProcessor(MappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	public String getMappingContextBeanName() {
		return mappingContextBeanName;
	}

	public void setMappingContextBeanName(String mappingContextBeanName) {
		this.mappingContextBeanName = mappingContextBeanName;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof MappingContextAware) {
			if (null == mappingContext) {
				Map<String, MappingContext> mappingContexts = applicationContext.getBeansOfType(MappingContext.class);
				if (mappingContexts.containsKey(mappingContextBeanName)) {
					this.mappingContext = mappingContexts.get(mappingContextBeanName);
				} else {
					String firstBean = mappingContexts.keySet().iterator().next();
					this.mappingContext = applicationContext.getBean(firstBean, MappingContext.class);
				}
			}
			((MappingContextAware) bean).setMappingContext(mappingContext);
		}
		return bean;
	}
}
