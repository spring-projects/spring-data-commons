package org.springframework.data.auditing;

import java.util.Date;

import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Sample entity using annotation based auditing.
 * 
 * @author Oliver Gierke
 * @since 1.5
 */
class AnnotatedUser {

	@CreatedBy
	Object createdBy;

	@CreatedDate
	DateTime createdDate;

	@LastModifiedBy
	Object lastModifiedBy;

	@LastModifiedDate
	Date lastModifiedDate;
}
