/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.web;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.xml.sax.SAXParseException;
import org.xmlbeam.XBProjector;
import org.xmlbeam.config.DefaultXMLFactoriesConfig;

/**
 * A read-only {@link HttpMessageConverter} to create XMLBeam-based projection instances for interfaces.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @see <a href="https://www.xmlbeam.org">https://www.xmlbeam.org</a>
 * @soundtrack Dr. Kobayashi Maru & The Mothership Connection - Anthem (EPisode One)
 */
public class XmlBeamHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	private final XBProjector projectionFactory;
	private final Map<Class<?>, Boolean> supportedTypesCache = new ConcurrentReferenceHashMap<>();

	/**
	 * Creates a new {@link XmlBeamHttpMessageConverter}.
	 */
	public XmlBeamHttpMessageConverter() {

		this(new XBProjector(new DefaultXMLFactoriesConfig() {

			private static final long serialVersionUID = -1324345769124477493L;

			/*
			 * (non-Javadoc)
			 * @see org.xmlbeam.config.DefaultXMLFactoriesConfig#createDocumentBuilderFactory()
			 */
			@Override
			public DocumentBuilderFactory createDocumentBuilderFactory() {

				DocumentBuilderFactory factory = super.createDocumentBuilderFactory();

				factory.setAttribute("http://apache.org/xml/features/disallow-doctype-decl", true);
				factory.setAttribute("http://xml.org/sax/features/external-general-entities", false);

				return factory;
			}
		}));
	}

	/**
	 * Creates a new {@link XmlBeamHttpMessageConverter} using the given {@link XBProjector}.
	 *
	 * @param projector must not be {@literal null}.
	 */
	public XmlBeamHttpMessageConverter(XBProjector projector) {

		super(MediaType.APPLICATION_XML, MediaType.parseMediaType("application/*+xml"));

		Assert.notNull(projector, "XBProjector must not be null!");

		this.projectionFactory = projector;
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

		try {

			return projectionFactory.io().stream(inputMessage.getBody()).read(clazz);

		} catch (RuntimeException o_O) {

			Throwable cause = o_O.getCause();

			if (SAXParseException.class.isInstance(cause)) {
				throw new HttpMessageNotReadableException("Cannot read input message!", cause, inputMessage);
			} else {
				throw o_O;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
	 */
	@Override
	protected void writeInternal(Object t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {}
}
