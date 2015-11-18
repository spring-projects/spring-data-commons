package org.springframework.data.transaction.service.impl;

import java.util.Date;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.transaction.service.AuditService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditServiceImpl implements AuditService {
	
	private static final Logger logger = LoggerFactory.getLogger(AuditService.class);	
	
	private JdbcTemplate jdbcTemplate;	
	

	@Autowired
	public void setDataSources(@Qualifier("secondDataSource") DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}	
	
	
    @Transactional(propagation = Propagation.NOT_SUPPORTED)      
    @Override	
	public int countAudits() {
		
		int count = jdbcTemplate.queryForObject("select count(*) from T_AUDITS", Integer.class);		
		
		return count;
		
	}
	
    
    @Transactional(propagation = Propagation.REQUIRED)      
    @Override	    
    public void createAudit() {
    	
		logger.debug("inserting a row into table T_AUDITS");			
		jdbcTemplate.update(
						"INSERT into T_AUDITS (id,operation,name,audit_date) values (?,?,?,?)",
						0, "INSERT", "foo", new Date());	    	
    	
    }    
    
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)      
    @Override	    
    public void deleteAudits() {
    	
		logger.debug("deleting all data from table T_AUDITS");			
		jdbcTemplate.update("delete from T_AUDITS");	    	
    	
    }    
        
    
    
    

}
