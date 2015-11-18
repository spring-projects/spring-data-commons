package org.springframework.data.transaction;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.transaction.service.AuditService;
import org.springframework.data.transaction.service.FooService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Some integration tests to test the ChainedTransactionManager
 * 
 * Currently there are two simple tests that are functionally the same apart from the methods to count the rows in the tables
 * using different transaction propagation.
 * 
 * 
 * The second test testCountFoosWithNotSupportedTransactionPropagationFollowedByCreateAFoo() fails with an exception 
 * 
 * CannotCreateTransactionException - Could not open JDBC Connection for transaction; 
 * nested exception is java.lang.IllegalStateException: Already value [org.springframework.jdbc.datasource.ConnectionHolder@d2de489] 
 * for key [org.apache.commons.dbcp.BasicDataSource@3f4faf53] bound to thread [main]
 * 
 * TJ - I think there is an issue with the ChainedTransactionManager possibly in the getTransaction() method where the synchronizationManager
 * is initialised regardless of the transaction propagation. Compare this with AbstractPlatformTransactionManager.getTransaction() 
 * where initSynchronization() would only be called for transaction propagation of REQUIRED, REQUIRES_NEW and NESTED 
 * 
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/META-INF/spring/transaction/data-source-context.xml")
public class ChainedTransactionManagerIntegrationTests {
	
	private static final Logger logger = LoggerFactory.getLogger(ChainedTransactionManagerIntegrationTests.class);	
	
	@Autowired	
	private FooService fooService;

	@Autowired	
	private AuditService auditService;	



	@Before
	public void clearData() {
				
		fooService.deleteFoos();
		
		auditService.deleteAudits();
	
	}

	
	/**
	 * A simple test that
	 * 
	 * 1) counts the number of rows in table T_FOOS then
	 * 
	 * 2) inserts one row into table T_FOOS then
	 * 
	 * 3) re-counts the number of rows in table T_FOOS
	 * 
	 * 
	 * Note that the method to count the rows in table T_FOOS are advised with REQUIRED transaction propagation 
	 * 
	 */
	@Test
	public void testCountFoosWithRequiredTransactionPropagationFollowedByCreateAFoo() {
		
		logger.debug("======== Test count Foos (with REQUIRED transaction propagation) followed by creating a Foo ==========");
		
		int countFoos = fooService.countFoosWithRequiredTransactionPropagation();
		
		logger.debug(countFoos + " rows of data in table T_FOOS");	
		assertEquals("There should be 0 rows in table T_FOOS", 0, countFoos);
				
		fooService.createAFoo();
		
		countFoos = fooService.countFoosWithRequiredTransactionPropagation();
		
		logger.debug(countFoos + " rows of data in table T_FOOS");	
		assertEquals("There should be one row in table T_FOOS", 1, countFoos);		
		
	}		
	
	
	/**
	 * This test is the same as the test above testCountFoosWithRequiredTransactionPropagationFollowedByCreateAFoo() except
	 * the the method to count the rows in table T_FOOS are advised with NOT_SUPPORTED transaction propagation (rather than REQUIRED)
	 * 
	 * A simple test that
	 * 
	 * 1) counts the number of rows in table T_FOOS then
	 * 
	 * 2) inserts one row into table T_FOOS then
	 * 
	 * 3) re-counts the number of rows in table T_FOOS
	 * 
	 * 
	 * Currently this test fails with the following exception:
	 * 	
	 * org.springframework.transaction.CannotCreateTransactionException: Could not open JDBC Connection for transaction; nested exception is java.lang.IllegalStateException: Already value [org.springframework.jdbc.datasource.ConnectionHolder@d2de489] for key [org.apache.commons.dbcp.BasicDataSource@3f4faf53] bound to thread [main]; nested exception is org.springframework.transaction.CannotCreateTransactionException: Could not open JDBC Connection for transaction; nested exception is java.lang.IllegalStateException: Already value [org.springframework.jdbc.datasource.ConnectionHolder@d2de489] for key [org.apache.commons.dbcp.BasicDataSource@3f4faf53] bound to thread [main]
	 * at org.springframework.data.transaction.ChainedTransactionManager.getTransaction(ChainedTransactionManager.java:122)
	 * at org.springframework.data.transaction.ChainedTransactionManager.getTransaction(ChainedTransactionManager.java:1)
	 * .
	 * .
	 * .
	 * 
	 */
	@Test
	public void testCountFoosWithNotSupportedTransactionPropagationFollowedByCreateAFoo() {
		
		logger.debug("======== Test count Foos (with NOT_SUPPORTED transaction propagation) followed by creating a Foo ==========");
		
		int countFoos = fooService.countFoosWithNotSupportedTransactionPropagation();
		
		logger.debug(countFoos + " rows of data in table T_FOOS");	
		assertEquals("There should be 0 rows in table T_FOOS", 0, countFoos);
				
		fooService.createAFoo();
		
		countFoos = fooService.countFoosWithNotSupportedTransactionPropagation();
		
		logger.debug(countFoos + " rows of data in table T_FOOS");	
		assertEquals("There should be one row in table T_FOOS", 1, countFoos);		
		
	}		
	
	
	

}
