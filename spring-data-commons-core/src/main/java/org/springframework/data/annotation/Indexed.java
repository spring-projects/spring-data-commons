package org.springframework.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotated fields and entities will be indexed and available for retrieval using an indexing API.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( value = {ElementType.FIELD,ElementType.TYPE})
public @interface Indexed
{
    /**
     * Name of the index to use.
     */
    String indexName() default "";
}
