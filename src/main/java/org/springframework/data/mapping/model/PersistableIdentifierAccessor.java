package org.springframework.data.mapping.model;

import org.springframework.data.domain.Persistable;
import org.springframework.data.mapping.IdentifierAccessor;

/**
 * {@link IdentifierAccessor} that invokes {@link Persistable#getId()}.
 * 
 * @author Oliver Gierke
 */
class PersistableIdentifierAccessor implements IdentifierAccessor {

	private final Persistable<?> target;

	/**
	 * Creates a new {@link PersistableIdentifierAccessor} for the given target.
	 * 
	 * @param target must not be {@literal null}.
	 */
	public PersistableIdentifierAccessor(Persistable<?> target) {
		this.target = target;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.IdentifierAccessor#getIdentifier()
	 */
	@Override
	public Object getIdentifier() {
		return target.getId();
	}
}
