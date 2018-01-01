/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.web;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.xmlbeam.XBProjector;

/**
 * A read-only {@link HttpMessageConverter} to create XMLBeam-based projection instances for interfaces.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @see <a href="http://www.xmlbeam.org">http://www.xmlbeam.org</a>
 * @soundtrack Dr. Kobayashi Maru & The Mothership Connection - Anthem (EPisode One)
 */
public class XmlBeamHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	private final XBProjector projectionFactory;
	private final Map<Class<?>, Boolean> supportedTypesCache = new ConcurrentReferenceHashMap<>();

	/**
	 * Creates a new {@link XmlBeamHttpMessageConverter}.
	 */
	public XmlBeamHttpMessageConverter() {

		super(MediaType.APPLICATION_XML, MediaType.parseMediaType("application/*+xml"));

		this.projectionFactory = new XBProjector();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#supports(java.lang.Class)
	 */
	@Override
	protected boolean supports(Class<?> type) {

		Class<?> rawType = ResolvableType.forType(type).resolve(Object.class);
		Boolean result = supportedTypesCache.get(rawType);

		if (result != null) {
			return result;
		}

		result = rawType.isInterface() && AnnotationUtils.findAnnotation(rawType, ProjectedPayload.class) != null;

		supportedTypesCache.put(rawType, result);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.HttpMessageConverter#canWrite(java.lang.Class, org.springframework.http.MediaType)
	 */
	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#readInternal(java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	@Override
	protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		return projectionFactory.io().stream(inputMessage.getBody()).read(clazz);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
	 */
	@Override
	protected void writeInternal(Object t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {}
}
