package org.springframework.data.util;

import java.util.Optional;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

public class TypeInformationAssert extends AbstractAssert<TypeInformationAssert, TypeInformation<?>> {

	/**
	 * @param actual
	 * @param selfType
	 */
	public TypeInformationAssert(TypeInformation<?> actual) {
		super(actual, TypeInformationAssert.class);
	}

	public static TypeInformationAssert assertThat(TypeInformation<?> information) {
		return new TypeInformationAssert(information);
	}

	public TypeInformationAssert hasComponentType(Class<?> type) {

		Assertions.assertThat(actual.getComponentType()).hasValueSatisfying(it -> {
			Assertions.assertThat(it.getType()).isEqualTo(type);
		});

		return this;
	}

	public AbstractObjectAssert<?, TypeInformation<?>> hasProperty(String property) {

		Optional<TypeInformation<?>> property2 = actual.getProperty(property);

		return Assertions.assertThat(property2.orElseGet(() -> {
			failWithMessage("Property %s not found!", property);
			return null;
		}));
	}
}
