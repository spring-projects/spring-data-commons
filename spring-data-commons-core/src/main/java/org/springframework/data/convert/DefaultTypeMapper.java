/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link MongoTypeMapper} allowing configuration of the key to lookup and store type
 * information in {@link DBObject}. The key defaults to {@link #DEFAULT_TYPE_KEY}. Actual type-to-{@link String}
 * conversion and back is done in {@link #getTypeString(TypeInformation)} or {@link #getTypeInformation(String)}
 * respectively.
 * 
 * @author Oliver Gierke
 */
public class DefaultTypeMapper<S> implements TypeMapper<S> {

	private final TypeAliasAccessor<S> accessor;
	private final List<? extends TypeInformationMapper> mappers;

	public DefaultTypeMapper(TypeAliasAccessor<S> accessor) {
		this(accessor, Arrays.asList(SimpleTypeInformationMapper.INSTANCE));
	}

	public DefaultTypeMapper(TypeAliasAccessor<S> accessor, List<? extends TypeInformationMapper> mappers) {

		this(accessor, null, mappers);
	}

	public DefaultTypeMapper(TypeAliasAccessor<S> accessor,
			MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext,
			List<? extends TypeInformationMapper> additionalMappers) {

		Assert.notNull(accessor);

		List<TypeInformationMapper> mappers = new ArrayList<TypeInformationMapper>(additionalMappers.size() + 1);
		if (mappingContext != null) {
			mappers.add(new ConfigurableTypeInformationMapper(mappingContext));
		}
		mappers.addAll(additionalMappers);

		this.mappers = Collections.unmodifiableList(mappers);
		this.accessor = accessor;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mongodb.core.convert.MongoTypeMapper#readType(com.mongodb.DBObject)
	 */
	public TypeInformation<?> readType(S source) {

		Assert.notNull(source);
		Object alias = accessor.readAliasFrom(source);

		for (TypeInformationMapper mapper : mappers) {
			TypeInformation<?> type = mapper.resolveTypeFrom(alias);

			if (type != null) {
				return type;
			}
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.TypeMapper#readType(java.lang.Object, org.springframework.data.util.TypeInformation)
	 */
	@SuppressWarnings("unchecked")
	public <T> TypeInformation<? extends T> readType(S source, TypeInformation<T> basicType) {

		Assert.notNull(source);
		Class<?> documentsTargetType = getDefaultedTypeToBeUsed(source);

		if (documentsTargetType == null) {
			return basicType;
		}

		Class<T> rawType = basicType == null ? null : basicType.getType();

		boolean isMoreConcreteCustomType = rawType == null ? true : rawType.isAssignableFrom(documentsTargetType)
				&& !rawType.equals(documentsTargetType);
		return isMoreConcreteCustomType ? (TypeInformation<? extends T>) ClassTypeInformation.from(documentsTargetType)
				: basicType;
	}

	/**
	 * Returns the type discovered through {@link #readType(Object)} but defaulted to the one returned by
	 * {@link #getFallbackTypeFor(Object)}.
	 * 
	 * @param source
	 * @return
	 */
	private Class<?> getDefaultedTypeToBeUsed(S source) {

		TypeInformation<?> documentsTargetTypeInformation = readType(source);
		documentsTargetTypeInformation = documentsTargetTypeInformation == null ? getFallbackTypeFor(source)
				: documentsTargetTypeInformation;
		return documentsTargetTypeInformation == null ? null : documentsTargetTypeInformation.getType();
	}

	/**
	 * Returns the type fallback {@link TypeInformation} in case none could be extracted from the given source.
	 * 
	 * @param source will never be {@literal null}.
	 * @return
	 */
	protected TypeInformation<?> getFallbackTypeFor(S source) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mongodb.core.convert.MongoTypeMapper#writeType(java.lang.Class, com.mongodb.DBObject)
	 */
	public void writeType(Class<?> type, S dbObject) {
		writeType(ClassTypeInformation.from(type), dbObject);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mongodb.core.convert.MongoTypeMapper#writeType(java.lang.Class, com.mongodb.DBObject)
	 */
	public void writeType(TypeInformation<?> info, S sink) {

		Assert.notNull(info);

		for (TypeInformationMapper mapper : mappers) {
			Object alias = mapper.createAliasFor(info);
			if (alias != null) {
				accessor.writeTypeTo(sink, alias);
				return;
			}
		}
	}
}
