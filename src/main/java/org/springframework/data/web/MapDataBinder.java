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
package org.springframework.data.web;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.AbstractPropertyAccessor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.util.Assert;
import org.springframework.web.bind.WebDataBinder;

/**
 * A {@link WebDataBinder} that automatically binds all properties exposed in the given type using a {@link Map}.
 *
 * @author Oliver Gierke
 * @since 1.10
 */
class MapDataBinder extends WebDataBinder {

	private final Class<?> type;
	private final ConversionService conversionService;

	/**
	 * Creates a new {@link MapDataBinder} for the given type and {@link ConversionService}.
	 * 
	 * @param type target type to detect property that need to be bound.
	 * @param conversionService the {@link ConversionService} to be used to preprocess values.
	 */
	public MapDataBinder(Class<?> type, ConversionService conversionService) {

		super(new HashMap<String, Object>());

		this.type = type;
		this.conversionService = conversionService;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.validation.DataBinder#getTarget()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> getTarget() {
		return (Map<String, Object>) super.getTarget();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.validation.DataBinder#getPropertyAccessor()
	 */
	@Override
	protected ConfigurablePropertyAccessor getPropertyAccessor() {
		return new MapPropertyAccessor(type, getTarget(), conversionService);
	}

	/**
	 * {@link PropertyAccessor} to store and retrieve values in a {@link Map}. Uses Spring Expression language to create
	 * deeply nested Map structures.
	 *
	 * @author Oliver Gierke
	 * @since 1.10
	 */
	private static class MapPropertyAccessor extends AbstractPropertyAccessor {

		private static final SpelExpressionParser PARSER = new SpelExpressionParser(
				new SpelParserConfiguration(false, true));

		private final Class<?> type;
		private final Map<String, Object> map;
		private final ConversionService conversionService;

		/**
		 * Creates a new {@link MapPropertyAccessor} for the given type, map and {@link ConversionService}.
		 * 
		 * @param type must not be {@literal null}.
		 * @param map must not be {@literal null}.
		 * @param conversionService must not be {@literal null}.
		 */
		public MapPropertyAccessor(Class<?> type, Map<String, Object> map, ConversionService conversionService) {

			Assert.notNull(type, "Type must not be null!");
			Assert.notNull(map, "Map must not be null!");
			Assert.notNull(conversionService, "ConversionService must not be null!");

			this.type = type;
			this.map = map;
			this.conversionService = conversionService;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.beans.PropertyAccessor#isReadableProperty(java.lang.String)
		 */
		@Override
		public boolean isReadableProperty(String propertyName) {
			throw new UnsupportedOperationException();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.beans.PropertyAccessor#isWritableProperty(java.lang.String)
		 */
		@Override
		public boolean isWritableProperty(String propertyName) {

			try {
				return getPropertyPath(propertyName) != null;
			} catch (PropertyReferenceException o_O) {
				return false;
			}
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.beans.PropertyAccessor#getPropertyTypeDescriptor(java.lang.String)
		 */
		@Override
		public TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException {
			throw new UnsupportedOperationException();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.beans.AbstractPropertyAccessor#getPropertyValue(java.lang.String)
		 */
		@Override
		public Object getPropertyValue(String propertyName) throws BeansException {
			throw new UnsupportedOperationException();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.beans.AbstractPropertyAccessor#setPropertyValue(java.lang.String, java.lang.Object)
		 */
		@Override
		public void setPropertyValue(String propertyName, Object value) throws BeansException {

			if (!isWritableProperty(propertyName)) {
				throw new NotWritablePropertyException(type, propertyName);
			}

			StandardEvaluationContext context = new StandardEvaluationContext();
			context.addPropertyAccessor(new PropertyTraversingMapAccessor(type, new DefaultConversionService()));
			context.setTypeConverter(new StandardTypeConverter(conversionService));
			context.setRootObject(map);

			Expression expression = PARSER.parseExpression(propertyName);

			PropertyPath leafProperty = getPropertyPath(propertyName).getLeafProperty();
			TypeInformation<?> owningType = leafProperty.getOwningType();
			TypeInformation<?> propertyType = leafProperty.getTypeInformation();

			propertyType = propertyName.endsWith("]") ? propertyType.getActualType() : propertyType;

			if (conversionRequired(value, propertyType.getType())) {

				PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(owningType.getType(),
						leafProperty.getSegment());
				MethodParameter methodParameter = new MethodParameter(descriptor.getReadMethod(), -1);
				TypeDescriptor typeDescriptor = TypeDescriptor.nested(methodParameter, 0);

				value = conversionService.convert(value, TypeDescriptor.forObject(value), typeDescriptor);
			}

			expression.setValue(context, value);
		}

		private boolean conversionRequired(Object source, Class<?> targetType) {

			if (targetType.isInstance(source)) {
				return false;
			}

			return conversionService.canConvert(source.getClass(), targetType);
		}

		private PropertyPath getPropertyPath(String propertyName) {

			String plainPropertyPath = propertyName.replaceAll("\\[.*?\\]", "");
			return PropertyPath.from(plainPropertyPath, type);
		}

		/**
		 * A special {@link MapAccessor} that traverses properties on the configured type to automatically create nested Map
		 * and collection values as necessary.
		 * 
		 * @author Oliver Gierke
		 * @since 1.10
		 */
		private static final class PropertyTraversingMapAccessor extends MapAccessor {

			private final ConversionService conversionService;
			private Class<?> type;

			/**
			 * Creates a new {@link PropertyTraversingMapAccessor} for the given type and {@link ConversionService}.
			 * 
			 * @param type must not be {@literal null}.
			 * @param conversionService must not be {@literal null}.
			 */
			public PropertyTraversingMapAccessor(Class<?> type, ConversionService conversionService) {

				Assert.notNull(type, "Type must not be null!");
				Assert.notNull(conversionService, "ConversionService must not be null!");

				this.type = type;
				this.conversionService = conversionService;
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.context.expression.MapAccessor#canRead(org.springframework.expression.EvaluationContext, java.lang.Object, java.lang.String)
			 */
			@Override
			public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
				return true;
			}

			/* 
			 * (non-Javadoc)
			 * @see org.springframework.context.expression.MapAccessor#read(org.springframework.expression.EvaluationContext, java.lang.Object, java.lang.String)
			 */
			@Override
			@SuppressWarnings("unchecked")
			public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {

				PropertyPath path = PropertyPath.from(name, type);

				try {
					return super.read(context, target, name);
				} catch (AccessException o_O) {

					Object emptyResult = path.isCollection() ? CollectionFactory.createCollection(List.class, 0)
							: CollectionFactory.createMap(Map.class, 0);

					((Map<String, Object>) target).put(name, emptyResult);

					return new TypedValue(emptyResult, getDescriptor(path, emptyResult));
				} finally {
					this.type = path.getType();
				}
			}

			/**
			 * Returns the type descriptor for the given {@link PropertyPath} and empty value for that path.
			 * 
			 * @param path must not be {@literal null}.
			 * @param emptyValue must not be {@literal null}.
			 * @return
			 */
			private TypeDescriptor getDescriptor(PropertyPath path, Object emptyValue) {

				Class<?> actualPropertyType = path.getType();

				TypeDescriptor valueDescriptor = conversionService.canConvert(String.class, actualPropertyType)
						? TypeDescriptor.valueOf(String.class) : TypeDescriptor.valueOf(HashMap.class);

				return path.isCollection() ? TypeDescriptor.collection(emptyValue.getClass(), valueDescriptor)
						: TypeDescriptor.map(emptyValue.getClass(), TypeDescriptor.valueOf(String.class), valueDescriptor);

			}
		}
	}
}
