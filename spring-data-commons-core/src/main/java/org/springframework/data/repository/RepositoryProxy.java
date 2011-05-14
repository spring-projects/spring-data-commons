package org.springframework.data.repository;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to demarcate interfaces a repository proxy shall be created for. Annotating an interface with
 * {@link RepositoryProxy} will cause the same behaviour as extending {@link Repository}.
 * 
 * @see Repository
 * @author Oliver Gierke
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RepositoryProxy {

	/**
	 * The domain class the repository manages. Equivalent to the T type parameter in {@link Repository}.
	 * 
	 * @see Repository
	 * @return
	 */
	Class<?> domainClass();
	
	/**
	 * The id class of the entity the repository manages. Equivalent to the ID type parameter in {@link Repository}.
	 * 
	 * @see Repository
	 * @return
	 */
	Class<? extends Serializable> idClass();
}
