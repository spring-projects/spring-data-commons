package org.springframework.data.transaction.service;

public interface AuditService {

	public int countAudits();	
	
	
	public void createAudit();
	
	
    public void deleteAudits();	
	
}
