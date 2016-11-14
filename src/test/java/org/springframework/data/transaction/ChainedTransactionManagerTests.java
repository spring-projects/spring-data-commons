/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.transaction;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.transaction.ChainedTransactionManagerTests.TestPlatformTransactionManager.*;

import org.hamcrest.Factory;
import org.junit.Test;
import org.springframework.transaction.HeuristicCompletionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Integration tests for {@link ChainedTransactionManager}.
 * 
 * @author Michael Hunger
 * @author Oliver Gierke
 * @since 1.6
 */
public class ChainedTransactionManagerTests {

	ChainedTransactionManager tm;

	@Test
	public void shouldCompleteSuccessfully() throws Exception {

		TestPlatformTransactionManager transactionManager = createNonFailingTransactionManager("single");
		setupTransactionManagers(transactionManager);

		createAndCommitTransaction();

		assertThat(transactionManager).matches(tptm -> tptm.isCommitted());
	}

	@Test
	public void shouldThrowRolledBackExceptionForSingleTMFailure() throws Exception {

		setupTransactionManagers(createFailingTransactionManager("single"));

		assertThatExceptionOfType(HeuristicCompletionException.class)//
				.isThrownBy(() -> createAndCommitTransaction())//
				.matches(e -> e.getOutcomeState() == HeuristicCompletionException.STATE_ROLLED_BACK);
	}

	@Test
	public void shouldCommitAllRegisteredTransactionManagers() {

		TestPlatformTransactionManager first = createNonFailingTransactionManager("first");
		TestPlatformTransactionManager second = createNonFailingTransactionManager("second");

		setupTransactionManagers(first, second);
		createAndCommitTransaction();

		assertThat(first).matches(ptm -> ptm.isCommitted());
		assertThat(second).matches(ptm -> ptm.isCommitted());
	}

	@Test
	public void shouldCommitInReverseOrder() {

		TestPlatformTransactionManager first = createNonFailingTransactionManager("first");
		TestPlatformTransactionManager second = createNonFailingTransactionManager("second");

		setupTransactionManagers(first, second);
		createAndCommitTransaction();

		assertThat(second.getCommitTime()).isLessThanOrEqualTo(first.getCommitTime());
	}

	@Test
	public void shouldThrowMixedRolledBackExceptionForNonFirstTMFailure() throws Exception {

		setupTransactionManagers(TestPlatformTransactionManager.createFailingTransactionManager("first"),
				createNonFailingTransactionManager("second"));

		assertThatExceptionOfType(HeuristicCompletionException.class)//
				.isThrownBy(() -> createAndCommitTransaction())//
				.matches(e -> e.getOutcomeState() == HeuristicCompletionException.STATE_MIXED);
	}

	@Test
	public void shouldRollbackAllTransactionManagers() throws Exception {

		TestPlatformTransactionManager first = createNonFailingTransactionManager("first");
		TestPlatformTransactionManager second = createNonFailingTransactionManager("second");

		setupTransactionManagers(first, second);
		createAndRollbackTransaction();

		assertThat(first).matches(ptm -> ptm.wasRolledBack());
		assertThat(second).matches(ptm -> ptm.wasRolledBack());

	}

	@Test(expected = UnexpectedRollbackException.class)
	public void shouldThrowExceptionOnFailingRollback() throws Exception {

		PlatformTransactionManager first = createFailingTransactionManager("first");
		setupTransactionManagers(first);
		createAndRollbackTransaction();
	}

	private void setupTransactionManagers(PlatformTransactionManager... transactionManagers) {
		tm = new ChainedTransactionManager(new TestSynchronizationManager(), transactionManagers);
	}

	private void createAndRollbackTransaction() {
		MultiTransactionStatus transaction = tm.getTransaction(new DefaultTransactionDefinition());
		tm.rollback(transaction);
	}

	private void createAndCommitTransaction() {
		MultiTransactionStatus transaction = tm.getTransaction(new DefaultTransactionDefinition());
		tm.commit(transaction);
	}

	static class TestSynchronizationManager implements SynchronizationManager {

		private boolean synchronizationActive;

		public void initSynchronization() {
			synchronizationActive = true;
		}

		public boolean isSynchronizationActive() {
			return synchronizationActive;
		}

		public void clearSynchronization() {
			synchronizationActive = false;
		}
	}

	static class TestPlatformTransactionManager implements org.springframework.transaction.PlatformTransactionManager {

		private final String name;
		private Long commitTime;
		private Long rollbackTime;

		public TestPlatformTransactionManager(String name) {
			this.name = name;
		}

		@Factory
		static TestPlatformTransactionManager createFailingTransactionManager(String name) {
			return new TestPlatformTransactionManager(name + "-failing") {
				@Override
				public void commit(TransactionStatus status) throws TransactionException {
					throw new RuntimeException();
				}

				@Override
				public void rollback(TransactionStatus status) throws TransactionException {
					throw new RuntimeException();
				}
			};
		}

		@Factory
		static TestPlatformTransactionManager createNonFailingTransactionManager(String name) {
			return new TestPlatformTransactionManager(name + "-non-failing");
		}

		@Override
		public String toString() {
			return name + (isCommitted() ? " (committed) " : " (not committed)");
		}

		public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
			return new TestTransactionStatus(definition);
		}

		public void commit(TransactionStatus status) throws TransactionException {
			commitTime = System.currentTimeMillis();
		}

		public void rollback(TransactionStatus status) throws TransactionException {
			rollbackTime = System.currentTimeMillis();
		}

		public boolean isCommitted() {
			return commitTime != null;
		}

		public boolean wasRolledBack() {
			return rollbackTime != null;
		}

		public Long getCommitTime() {
			return commitTime;
		}

		static class TestTransactionStatus implements org.springframework.transaction.TransactionStatus {

			public TestTransactionStatus(TransactionDefinition definition) {}

			public boolean isNewTransaction() {
				return false;
			}

			public boolean hasSavepoint() {
				return false;
			}

			public void setRollbackOnly() {

			}

			public boolean isRollbackOnly() {
				return false;
			}

			public void flush() {

			}

			public boolean isCompleted() {
				return false;
			}

			public Object createSavepoint() throws TransactionException {
				return null;
			}

			public void rollbackToSavepoint(Object savepoint) throws TransactionException {

			}

			public void releaseSavepoint(Object savepoint) throws TransactionException {

			}
		}
	}
}
