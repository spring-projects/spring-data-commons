package org.springframework.data.transaction.service;

public interface FooService {
	
	public int countFoosWithNotSupportedTransactionPropagation();	
	
	
	public int countFoosWithRequiredTransactionPropagation();
	
	
	public void createAFoo();
	
	
	public void updateAFoo();
	
	
    public void deleteFoos();	
	
}
