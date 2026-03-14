package org.springframework.data.querydsl;

import com.querydsl.core.annotations.PropertyType;
import com.querydsl.core.annotations.QueryEntity;
import com.querydsl.core.annotations.QueryType;

/**
 * Test domain type for GH-2176.
 * @author Kamil Krzywański
 */
@QueryEntity
public class Example {

	public String one;

	@QueryType(PropertyType.NONE)
	public String two;

	public transient String three;
}