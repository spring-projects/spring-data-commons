/*
 * Copyright 2014-2024 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.geo.GeoModule;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializerBase;
import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * JavaConfig class to export Jackson specific configuration.
 *
 * @author Oliver Gierke
 */
public class SpringDataJacksonConfiguration implements SpringDataJacksonModules {

	@Bean
	public GeoModule jacksonGeoModule() {
		return new GeoModule();
	}

	@Bean
	public PageModule pageModule() {
		return new PageModule();
	}

	public static class PageModule extends SimpleModule {

		private static final long serialVersionUID = 275254460581626332L;

		private static final String UNPAGED_TYPE_NAME = "org.springframework.data.domain.Unpaged";
		private static final Class<?> UNPAGED_TYPE;

		static {
			UNPAGED_TYPE = ClassUtils.resolveClassName(UNPAGED_TYPE_NAME, PageModule.class.getClassLoader());
		}

		public PageModule() {
			addSerializer(UNPAGED_TYPE, new UnpagedAsInstanceSerializer());
		}

		/**
		 * A Jackson serializer rendering instances of {@link org.springframework.data.domain.Unpaged} as {@code INSTANCE}
		 * as it was previous rendered.
		 *
		 * @author Oliver Drotbohm
		 */
		static class UnpagedAsInstanceSerializer extends ToStringSerializerBase {

			private static final long serialVersionUID = -1213451755610144637L;

			public UnpagedAsInstanceSerializer() {
				super(Object.class);
			}

			@Override
			public String valueToString(@Nullable Object value) {
				return "INSTANCE";
			}
		}
	}
}
