package uk.ac.warwick.dcs.sherlock.engine.storage;

import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;

import jakarta.persistence.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
<<<<<<< HEAD
			
=======
				
<<<<<<< HEAD
<<<<<<< HEAD
>>>>>>> aa5029eb (Corrected spelling mistake in comment)
=======
<<<<<<< HEAD
>>>>>>> 22d3a887 (Fixed BaseStorageTest.storeFile test)
=======
<<<<<<< HEAD
>>>>>>> 763083d5 (Corrected spelling mistake in comment)
				// If the object is an EntityArchive, merge its workspace first
=======
				// If the object is an EntityArchive, merge its workkspace first
>>>>>>> 359b5591 (Fixed BaseStorageTest.storeFile test)
=======
				// If the object is an EntityArchive, merge its workspace first
>>>>>>> 1a630032 (Corrected spelling mistake in comment)
				// to avoid foreign key constraint issues
				if (obj instanceof EntityArchive) {
					EntityArchive a = (EntityArchive) obj;

					if (a.getWorkspace() != null) {
						// This takes a detached entity and brings it to the current context
						// so it can be managed by ObjectDB and be safely removed later
						em.merge(a.getWorkspace());
					}
				}

				Object managed = em.merge(obj);
				em.remove(managed);

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
