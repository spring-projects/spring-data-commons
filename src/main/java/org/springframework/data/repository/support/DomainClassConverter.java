/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.data.repository.support;

import java.util.Collections;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.CrudInvoker;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.core.convert.converter.Converter} to convert arbitrary input into domain classes managed
 * by Spring Data {@link CrudRepository}s. The implementation uses a {@link ConversionService} in turn to convert the
 * source type into the domain class' id type which is then converted into a domain class object by using a
 * {@link CrudRepository}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class DomainClassConverter<T extends ConversionService & ConverterRegistry> implements
		ConditionalGenericConverter, ApplicationContextAware {

	private final T conversionService;
	private Repositories repositories = Repositories.NONE;

	public DomainClassConverter(T conversionService) {
		this.conversionService = conversionService;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
	 */
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (source == null || !StringUtils.hasText(source.toString())) {
			return null;
		}

		if(sourceType.equals(targetType)) {
			return source;
		}
		
		Class<?> domainType = targetType.getType();

		RepositoryInformation info = repositories.getRepositoryInformationFor(domainType);
		CrudInvoker<?> invoker = repositories.getCrudInvoker(domainType);

		return invoker.invokeFindOne(conversionService.convert(source, info.getIdType()));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.ConditionalGenericConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (!repositories.hasRepositoryFor(targetType.getType())) {
			return false;
		}
		
		if(sourceType.equals(targetType)) {
			return true;
		}

		return conversionService.canConvert(sourceType.getType(),
				repositories.getRepositoryInformationFor(targetType.getType()).getIdType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext context) {

		this.repositories = new Repositories(context);
		this.conversionService.addConverter(this);
	}
}
