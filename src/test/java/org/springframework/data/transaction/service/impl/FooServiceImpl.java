package org.springframework.data.transaction.service.impl;

import java.util.Date;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.transaction.service.FooService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FooServiceImpl implements FooService {
	
	private static final Logger logger = LoggerFactory.getLogger(FooService.class);	
	
	private JdbcTemplate jdbcTemplate;	
	

	@Autowired
	public void setDataSources(@Qualifier("firstDataSource") DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}	
	
	
    @Transactional(propagation = Propagation.NOT_SUPPORTED)      
    @Override	
	public int countFoosWithNotSupportedTransactionPropagation() {
		
		int count = jdbcTemplate.queryForObject("select count(*) from T_FOOS", Integer.class);		
		
		return count;
		
	}
    
    
    @Transactional(propagation = Propagation.REQUIRED)
    @Override	
	public int countFoosWithRequiredTransactionPropagation() {
		
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");		
		
		return count;
		
	}    
    
    

    @Transactional(propagation = Propagation.REQUIRED)      
    @Override	    
    public void createAFoo() {
    	
		logger.debug("inserting a row into from table T_FOOS");		
		jdbcTemplate.update(
				"INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", 0,
				"foo");		    	
    	
    }
	
	
    @Transactional(propagation = Propagation.REQUIRED)      
    @Override	    
    public void updateAFoo() {
    	
		logger.debug("updating a row in table T_FOOS");		
		jdbcTemplate.update(
				"UPDATE T_FOOS set name = ? WHERE id = 0", "an updated foo " + new Date());		    	
    	
    }
    
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)      
    @Override	    
    public void deleteFoos() {
    	
		logger.debug("deleting all data from table T_FOOS");			
		jdbcTemplate.update("delete from T_FOOS");	    	
    	
    }        
    
    
}
