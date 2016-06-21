package org.springframework.data.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CastUtils {

	@SuppressWarnings("unchecked")
	public <T> T cast(Object object) {
		return (T) object;
	}
}
