/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.data.web.config;


import java.io.Serial;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.geo.GeoModule;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializerBase;
import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * JavaConfig class to export Jackson specific configuration.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @deprecated since 4.0, in favor of {@link SpringDataJackson3Configuration} which uses Jackson 3.
 */
@SuppressWarnings("removal")
@Deprecated(since = "4.0", forRemoval = true)
public class SpringDataJacksonConfiguration implements SpringDataJacksonModules {

	@Nullable
	@Autowired(required = false) SpringDataWebSettings settings;

	@Bean
	public GeoModule jacksonGeoModule() {
		return new GeoModule();
	}

	@Bean
	public PageModule pageModule() {
		return new PageModule(settings);
	}

	/**
	 * A Jackson module customizing the serialization of {@link PageImpl} instances depending on the
	 * {@link SpringDataWebSettings} handed into the instance. In case of {@link PageSerializationMode#DIRECT} being
	 * configured, a no-op {@link StdConverter} is registered to issue a one-time warning about the mode being used (as
	 * it's not recommended). {@link PageSerializationMode#VIA_DTO} would register a converter wrapping {@link PageImpl}
	 * instances into {@link PagedModel}.
	 *
	 * @author Oliver Drotbohm
	 */
	public static class PageModule extends SimpleModule {

		private static final @Serial long serialVersionUID = 275254460581626332L;

		private static final String UNPAGED_TYPE_NAME = "org.springframework.data.domain.Unpaged";
		private static final Class<?> UNPAGED_TYPE;

		static {
			UNPAGED_TYPE = ClassUtils.resolveClassName(UNPAGED_TYPE_NAME, PageModule.class.getClassLoader());
		}

		/**
		 * Creates a new {@link PageModule} for the given {@link SpringDataWebSettings}.
		 *
		 * @param settings can be {@literal null}.
		 */
		public PageModule(@Nullable SpringDataWebSettings settings) {

			addSerializer(UNPAGED_TYPE, new UnpagedAsInstanceSerializer());

			if (settings == null || settings.pageSerializationMode() == PageSerializationMode.DIRECT) {
				setSerializerModifier(new WarningLoggingModifier());

			} else {
				setMixInAnnotation(PageImpl.class, WrappingMixing.class);
			}
		}

		/**
		 * A Jackson serializer rendering instances of {@code org.springframework.data.domain.Unpaged} as {@code INSTANCE}
		 * as it was previous rendered.
		 *
		 * @author Oliver Drotbohm
		 */
		static class UnpagedAsInstanceSerializer extends ToStringSerializerBase {

			private static final @Serial long serialVersionUID = -1213451755610144637L;

			public UnpagedAsInstanceSerializer() {
				super(Object.class);
			}

			@Override
			public String valueToString(@Nullable Object value) {
				return "INSTANCE";
			}
		}

		@JsonSerialize(converter = PageModelConverter.class)
		abstract static class WrappingMixing {}

		static class PageModelConverter extends StdConverter<Page<?>, PagedModel<?>> {

			@Override
			public @Nullable PagedModel<?> convert(@Nullable Page<?> value) {
				return value == null ? null : new PagedModel<>(value);
			}
		}

		/**
		 * A {@link BeanSerializerModifier} that logs a warning message if an instance of {@link Page} will be rendered.
		 *
		 * @author Oliver Drotbohm
		 */
		static class WarningLoggingModifier extends BeanSerializerModifier {

			private static final Logger LOGGER = LoggerFactory.getLogger(WarningLoggingModifier.class);
			private static final String MESSAGE = """
					Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee about the stability of the resulting JSON structure!
						For a stable JSON structure, please use Spring Data's PagedModel (globally via @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO))
						or Spring HATEOAS and Spring Data's PagedResourcesAssembler as documented in https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web.pageables.
					""";

			private static final @Serial long serialVersionUID = 954857444010009875L;

			private boolean warningRendered = false;

			@Override
			public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
					List<BeanPropertyWriter> beanProperties) {

				if (Page.class.isAssignableFrom(beanDesc.getBeanClass()) && !warningRendered) {

					this.warningRendered = true;
					LOGGER.warn(MESSAGE);
				}

				return super.changeProperties(config, beanDesc, beanProperties);
			}
		}
	}

}
