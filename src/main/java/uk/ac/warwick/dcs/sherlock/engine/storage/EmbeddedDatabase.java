package uk.ac.warwick.dcs.sherlock.engine.storage;

import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;

import jakarta.persistence.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Database access stuff
 */
public class EmbeddedDatabase {

	private EntityManagerFactory dbFactory;
	private EntityManager em;

	public EmbeddedDatabase() {
		String dataPath = SherlockEngine.configuration.getDataPath();
		File dataDir = new File(dataPath);
		if (!dataDir.exists() && !dataDir.mkdirs()) {
			throw new IllegalStateException("Failed to create data directory: " + dataDir.getAbsolutePath());
		}

		System.setProperty("objectdb.home", dataDir.getAbsolutePath());
		File logDir = new File(dataDir, "log");
		if (!logDir.exists() && !logDir.mkdirs()) {
    		throw new IllegalStateException("Failed to create log directory: " + logDir.getAbsolutePath());
		}

		Map<String, String> properties = new HashMap<>();
		properties.put("jakarta.persistence.jdbc.user", "admin");
		properties.put("jakarta.persistence.jdbc.password", "admin");
		File dbFile = new File(dataDir, "Sherlock.odb");
		properties.put("jakarta.persistence.jdbc.url", "objectdb:" + dbFile.getAbsolutePath());

		this.dbFactory = Persistence.createEntityManagerFactory("objectdb", properties);
		this.em = this.dbFactory.createEntityManager();
		this.em.flush();
	}

	public void close() {
		this.em.close();
		this.dbFactory.close();
	}

	public Query createQuery(String query) {
		return em.createQuery(query);
	}

	public <X> TypedQuery<X> createQuery(String query, Class<X> xclass) {
		return em.createQuery(query, xclass);
	}

	public int executeUpdate(Query query) {
		if (query != null) {
			return this.runInTransaction(query::executeUpdate);
		}

		return -1;
	}

	public void refreshObject(Object obj) {
		if (obj != null && this.em.contains(obj)) {
			this.em.refresh(obj);
		}
	}

	public void removeObject(Object obj) {
		if (obj instanceof List) {
			this.removeObject(((List) obj).toArray());
		}
		else {
			this.runInTransaction(() -> em.remove(this.getManagedObject(obj)));
		}
	}

	public void removeObject(Object... objects) {
		this.runInTransaction(() -> {
			for (Object obj : objects) {
				em.remove(this.getManagedObject(obj));
			}
		});
	}

	public <X> List<X> runQuery(String query, Class<X> xclass) {
		List<X> q = em.createQuery(query, xclass).getResultList();
		return q;
	}

	public void storeObject(Object obj) {
		if (obj instanceof List) {
			this.storeObject(((List) obj).toArray());
		}
		else {
			this.runInTransaction(() -> em.persist(obj));
		}
	}

	public void storeObject(Object... objects) {
		this.runInTransaction(() -> {
			for (Object obj : objects) {
				em.persist(obj);
			}
		});
	}

	private Object getManagedObject(Object obj) {
		return em.contains(obj) ? obj : em.merge(obj);
	}

	private void runInTransaction(Runnable operation) {
		this.runInTransaction(() -> {
			operation.run();
			return null;
		});
	}

	private <T> T runInTransaction(Supplier<T> operation) {
		EntityTransaction transaction = em.getTransaction();
		boolean startedTransaction = false;

		try {
			if (!transaction.isActive()) {
				transaction.begin();
				startedTransaction = true;
			}

			T result = operation.get();

			if (startedTransaction) {
				transaction.commit();
			}

			return result;
		}
		catch (RuntimeException e) {
			if (startedTransaction) {
				this.rollback(transaction, e);
			}
			throw e;
		}
	}

	private void rollback(EntityTransaction transaction, RuntimeException cause) {
		try {
			if (transaction.isActive()) {
				transaction.rollback();
			}
		}
		catch (RuntimeException rollbackException) {
			cause.addSuppressed(rollbackException);
		}
	}

}
