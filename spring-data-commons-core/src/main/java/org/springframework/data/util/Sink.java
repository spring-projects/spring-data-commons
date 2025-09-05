package org.springframework.data.util;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * A simple {@link Consumer} that captures the instance handed into it.
 *
 * @author Oliver Drotbohm
 * @since 2.4.12
 */
class Sink<T> implements Consumer<T> {

	private @Nullable T value;

	/**
	 * Returns the value captured.
	 *
	 * @return
	 */
	public @Nullable T getValue() {
		return value;
	}

	@Override
	public void accept(@Nullable T t) {
		this.value = t;
	}
}
