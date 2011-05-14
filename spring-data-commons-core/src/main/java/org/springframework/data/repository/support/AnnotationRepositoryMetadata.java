package org.springframework.data.repository.support;

import org.springframework.data.repository.RepositoryProxy;
import org.springframework.util.Assert;

/**
 * {@link RepositoryMetadata} implementation inspecting the given repository interface for a {@link RepositoryProxy}
 * annotation.
 * 
 * @author Oliver Gierke
 */
public class AnnotationRepositoryMetadata implements RepositoryMetadata {
	
	private static final String NO_ANNOTATION_FOUND = String.format("Interface must be annotated with @%s!",
			RepositoryProxy.class.getName());
	
	private final Class<?> repositoryInterface;
	
	public AnnotationRepositoryMetadata(Class<?> repositoryInterface) {
		Assert.notNull(repositoryInterface, "Repository interface must not be null!");
		Assert.isTrue(repositoryInterface.isAnnotationPresent(RepositoryProxy.class), NO_ANNOTATION_FOUND);
		this.repositoryInterface = repositoryInterface;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getIdClass()
	 */
	public Class<?> getIdClass() {
		RepositoryProxy annotation = repositoryInterface.getAnnotation(RepositoryProxy.class);
		return annotation == null ? null : annotation.idClass();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getDomainClass()
	 */
	public Class<?> getDomainClass() {
		RepositoryProxy annotation = repositoryInterface.getAnnotation(RepositoryProxy.class);
		return annotation == null ? null : annotation.domainClass();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getRepositoryInterface()
	 */
	public Class<?> getRepositoryInterface() {
		return repositoryInterface;
	}
}
