package org.springframework.data.auditing.config;

/**
 * Configuration information for auditing.
 *
 * @author Ranie Jade Ramiso
 */
public interface AnnotationAuditingConfiguration {
	String getAuditorAwareRef();

	boolean isSetDates();

	boolean isModifyOnCreate();

	String getDateTimeProviderRef();
}
