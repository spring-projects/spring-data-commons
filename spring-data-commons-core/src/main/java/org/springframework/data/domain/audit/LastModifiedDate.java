package org.springframework.data.domain.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Ranie Jade Ramiso
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.FIELD })
public @interface LastModifiedDate {
}
