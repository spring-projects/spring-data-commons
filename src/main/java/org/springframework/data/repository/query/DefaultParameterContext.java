package org.springframework.data.repository.query;

import java.util.function.Supplier;

import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider.ParameterContext;
import org.springframework.data.util.Lazy;

/**
 * Default {@link ParameterContext} implementation.
 *
 * @author Mark Paluch
 * @since 2.1
 */
class DefaultParameterContext<T extends Parameters<?, ?>> implements ParameterContext<T> {

	private final T parameters;
	private final Lazy<Object[]> parameterValues;

	DefaultParameterContext(T parameters, Object[] parameterValues) {
		this(parameters, Lazy.of(() -> parameterValues));
	}

	DefaultParameterContext(T parameters, Supplier<Object[]> parameterValues) {
		this(parameters, Lazy.of(parameterValues));
	}

	private DefaultParameterContext(T parameters, Lazy<Object[]> parameterValues) {

		this.parameters = parameters;
		this.parameterValues = parameterValues;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethodEvaluationContextProvider.ParameterContext#getParameters()
	 */
	@Override
	public T getParameters() {
		return parameters;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethodEvaluationContextProvider.ParameterContext#getParameterValues()
	 */
	@Override
	public Object[] getParameterValues() {
		return parameterValues.get();
	}
}
