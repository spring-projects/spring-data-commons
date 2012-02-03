package org.springframework.data.persistence;

import org.springframework.data.convert.EntityInstantiator;

/**
 * encapsulates the instantiator of state-backed classes and populating them with the provided state.
 * <p/>
 * Can be implemented and registered with the concrete AbstractConstructorEntityInstantiator to provide non reflection
 * bases instantiaton for domain classes
 * 
 * @deprecated use {@link EntityInstantiator} abstraction instead.
 */
@Deprecated
public interface StateBackedCreator<T, STATE> {
	T create(STATE n, Class<T> c) throws Exception;
}
