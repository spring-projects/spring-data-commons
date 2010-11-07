package org.springframework.data.repository.query;

/**
 * Exception to be thrown if a query cannot be created from a
 * {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 */
public final class QueryCreationException extends RuntimeException {

    private static final long serialVersionUID = -1238456123580L;
    private static final String MESSAGE_TEMPLATE =
            "Could not create query for method %s! Could not find property %s on domain class %s.";


    /**
     * Creates a new {@link QueryCreationException}.
     * 
     * @param method
     */
    private QueryCreationException(String message) {

        super(message);
    }


    /**
     * Rejects the given domain class property.
     * 
     * @param method
     * @param propertyName
     * @return
     */
    public static QueryCreationException invalidProperty(QueryMethod method,
            String propertyName) {

        return new QueryCreationException(String.format(MESSAGE_TEMPLATE,
                method, propertyName, method.getDomainClass().getName()));
    }


    /**
     * Creates a new {@link QueryCreationException}.
     * 
     * @param method
     * @param message
     * @return
     */
    public static QueryCreationException create(QueryMethod method,
            String message) {

        return new QueryCreationException(String.format(
                "Could not create query for %s! Reason: %s", method, message));
    }


    /**
     * Creates a new {@link QueryCreationException} for the given
     * {@link QueryMethod} and {@link Throwable} as cause.
     * 
     * @param method
     * @param cause
     * @return
     */
    public static QueryCreationException create(QueryMethod method,
            Throwable cause) {

        return create(method, cause.getMessage());
    }
}
