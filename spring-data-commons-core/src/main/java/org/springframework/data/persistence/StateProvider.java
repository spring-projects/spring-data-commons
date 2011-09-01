package org.springframework.data.persistence;

/**
 * @author Michael Hunger
 * @since 24.09.2010
 */
public abstract class StateProvider {
	private final static ThreadLocal stateHolder = new ThreadLocal();

	private StateProvider() {
	}

	public static <STATE> void setUnderlyingState(STATE state) {
		if (stateHolder.get() != null)
			throw new IllegalStateException("StateHolder already contains state " + stateHolder.get() + " in thread "
					+ Thread.currentThread());
		stateHolder.set(state);
	}

	public static <STATE> STATE retrieveState() {
		STATE result = (STATE) stateHolder.get();
		stateHolder.remove();
		return result;
	}
}
