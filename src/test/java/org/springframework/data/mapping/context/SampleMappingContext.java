package org.springframework.data.mapping.context;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

public class SampleMappingContext
		extends AbstractMappingContext<BasicPersistentEntity<Object, SamplePersistentProperty>, SamplePersistentProperty> {

	@Override
	@SuppressWarnings("unchecked")
	protected <S> BasicPersistentEntity<Object, SamplePersistentProperty> createPersistentEntity(
			TypeInformation<S> typeInformation) {
		return new BasicPersistentEntity<Object, SamplePersistentProperty>((TypeInformation<Object>) typeInformation);
	}

	@Override
	protected SamplePersistentProperty createPersistentProperty(Property property,
			BasicPersistentEntity<Object, SamplePersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {

		return new SamplePersistentProperty(property, owner, simpleTypeHolder);
	}
}
