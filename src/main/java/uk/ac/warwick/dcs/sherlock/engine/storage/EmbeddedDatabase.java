package uk.ac.warwick.dcs.sherlock.engine.storage;

import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;

import jakarta.persistence.*;
import java.io.File;
import java.util.*;

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
		// Ensure ObjectDB writes outside the nested boot jar
		System.setProperty("objectdb.home", dataDir.getAbsolutePath());
		File logDir = new File(dataDir, "log");
		if (!logDir.exists()) {
			logDir.mkdirs();
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
			int count;
			try {
				em.getTransaction().begin();
				count = query.executeUpdate();
				em.getTransaction().commit();
			}
			finally {
				if (em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				}
			}
			return count;
		}

		return -1;
	}

	public void refreshObject(Object obj) {
		this.em.refresh(obj);
	}

	public void removeObject(Object obj) {
		if (obj instanceof List) {
			this.removeObject(((List) obj).toArray());
		}
		else {
			try {
				em.getTransaction().begin();
				em.remove(obj);
				em.getTransaction().commit();
			}
			finally {
				if (em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				}
			}
		}
	}

	public void removeObject(Object... objects) {
		try {
			em.getTransaction().begin();
			for (Object obj : objects) {
				em.remove(obj);
			}
			em.getTransaction().commit();
		}
		finally {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
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
			try {
				em.getTransaction().begin();
				em.persist(obj);
				em.getTransaction().commit();
			}
			finally {
				if (em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				}
			}
		}
	}

	public void storeObject(Object... objects) {
		try {
			em.getTransaction().begin();
			for (Object obj : objects) {
				em.persist(obj);
			}
			em.getTransaction().commit();
		}
		finally {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}
}
