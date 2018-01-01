/*
 * Copyright 2008-2018 the original author or authors.
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
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
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
public class DomainClassConverter<T extends ConversionService & ConverterRegistry>
		implements ConditionalGenericConverter, ApplicationContextAware {

	private final T conversionService;
	private Repositories repositories = Repositories.NONE;
	private Optional<ToEntityConverter> toEntityConverter = Optional.empty();
	private Optional<ToIdConverter> toIdConverter = Optional.empty();

	/**
	 * Creates a new {@link DomainClassConverter} for the given {@link ConversionService}.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public DomainClassConverter(T conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.conversionService = conversionService;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
	 */
	@Nonnull
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Nullable
	@Override
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return getConverter(targetType).map(it -> it.convert(source, sourceType, targetType)).orElse(source);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return getConverter(targetType).map(it -> it.matches(sourceType, targetType)).orElse(false);
	}

	/**
	 * @param targetType
	 * @return
	 */
	private Optional<? extends ConditionalGenericConverter> getConverter(TypeDescriptor targetType) {
		return repositories.hasRepositoryFor(targetType.getType()) ? toEntityConverter : toIdConverter;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext context) {

		this.repositories = new Repositories(context);

		this.toEntityConverter = Optional.of(new ToEntityConverter(this.repositories, this.conversionService));
		this.toEntityConverter.ifPresent(it -> this.conversionService.addConverter(it));

		this.toIdConverter = Optional.of(new ToIdConverter());
		this.toIdConverter.ifPresent(it -> this.conversionService.addConverter(it));
	}

	/**
	 * Converter to create domain types from any source that can be converted into the domain types identifier type.
	 *
	 * @author Oliver Gierke
	 * @since 1.10
	 */
	private class ToEntityConverter implements ConditionalGenericConverter {

		private final RepositoryInvokerFactory repositoryInvokerFactory;

		/**
		 * Creates a new {@link ToEntityConverter} for the given {@link Repositories} and {@link ConversionService}.
		 *
		 * @param repositories must not be {@literal null}.
		 * @param conversionService must not be {@literal null}.
		 */
		public ToEntityConverter(Repositories repositories, ConversionService conversionService) {
			this.repositoryInvokerFactory = new DefaultRepositoryInvokerFactory(repositories, conversionService);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
		 */
		@Nonnull
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
		 */
		@Nullable
		@Override
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

			if (source == null || !StringUtils.hasText(source.toString())) {
				return null;
			}

			if (sourceType.equals(targetType)) {
				return source;
			}

			Class<?> domainType = targetType.getType();
			RepositoryInvoker invoker = repositoryInvokerFactory.getInvokerFor(domainType);
			RepositoryInformation information = repositories.getRequiredRepositoryInformation(domainType);

			Object id = conversionService.convert(source, information.getIdType());

			return id == null ? null : invoker.invokeFindById(id).orElse(null);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
		 */
		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

			if (sourceType.isAssignableTo(targetType)) {
				return false;
			}

			Class<?> domainType = targetType.getType();

			if (!repositories.hasRepositoryFor(domainType)) {
				return false;
			}

			Optional<RepositoryInformation> repositoryInformation = repositories.getRepositoryInformationFor(domainType);

			return repositoryInformation.map(it -> {

				Class<?> rawIdType = it.getIdType();

				return sourceType.equals(TypeDescriptor.valueOf(rawIdType))
						|| conversionService.canConvert(sourceType.getType(), rawIdType);
			}).orElseThrow(
					() -> new IllegalStateException(String.format("Couldn't find RepositoryInformation for %s!", domainType)));
		}
	}

	/**
	 * Converter to turn domain types into their identifiers or any transitively convertible type.
	 *
	 * @author Oliver Gierke
	 * @since 1.10
	 */
	class ToIdConverter implements ConditionalGenericConverter {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
		 */
		@Nonnull
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
		 */
		@Nullable
		@Override
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

			if (source == null || !StringUtils.hasText(source.toString())) {
				return null;
			}

			if (sourceType.equals(targetType)) {
				return source;
			}

			Class<?> domainType = sourceType.getType();

			EntityInformation<Object, ?> entityInformation = repositories.getEntityInformationFor(domainType);

			return conversionService.convert(entityInformation.getId(source), targetType.getType());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
		 */
		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

			if (sourceType.isAssignableTo(targetType)) {
				return false;
			}

			Class<?> domainType = sourceType.getType();

			if (!repositories.hasRepositoryFor(domainType)) {
				return false;
			}

			Optional<RepositoryInformation> information = repositories.getRepositoryInformationFor(domainType);

			return information.map(it -> {

				Class<?> rawIdType = it.getIdType();

				return targetType.equals(TypeDescriptor.valueOf(rawIdType))
						|| conversionService.canConvert(rawIdType, targetType.getType());

			}).orElseThrow(
					() -> new IllegalStateException(String.format("Couldn't find RepositoryInformation for %s!", domainType)));
		}
	}
}
