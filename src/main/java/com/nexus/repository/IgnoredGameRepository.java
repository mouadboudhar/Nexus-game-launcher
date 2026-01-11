package com.nexus.repository;

import com.nexus.model.IgnoredGame;
import com.nexus.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository for IgnoredGame entity CRUD operations using Hibernate.
 */
public class IgnoredGameRepository {

    /**
     * Save or update an ignored game.
     */
    public IgnoredGame save(IgnoredGame ignoredGame) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            if (ignoredGame.getId() == null) {
                session.persist(ignoredGame);
            } else {
                ignoredGame = session.merge(ignoredGame);
            }
            transaction.commit();
            return ignoredGame;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Find an ignored game by ID.
     */
    public Optional<IgnoredGame> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(IgnoredGame.class, id));
        }
    }

    /**
     * Get all ignored games.
     */
    public List<IgnoredGame> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM IgnoredGame ORDER BY title", IgnoredGame.class).list();
        }
    }

    /**
     * Find an ignored game by its unique ID.
     */
    public Optional<IgnoredGame> findByUniqueId(String uniqueId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM IgnoredGame WHERE uniqueId = :uid", IgnoredGame.class)
                    .setParameter("uid", uniqueId)
                    .uniqueResultOptional();
        }
    }

    /**
     * Find an ignored game by install path.
     */
    public Optional<IgnoredGame> findByInstallPath(String installPath) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM IgnoredGame WHERE installPath = :path", IgnoredGame.class)
                    .setParameter("path", installPath)
                    .uniqueResultOptional();
        }
    }

    /**
     * Check if a game is ignored by its unique ID.
     */
    public boolean isIgnored(String uniqueId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("SELECT COUNT(ig) FROM IgnoredGame ig WHERE ig.uniqueId = :uid", Long.class)
                    .setParameter("uid", uniqueId)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    /**
     * Get all ignored unique IDs.
     */
    public List<String> findAllUniqueIds() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT ig.uniqueId FROM IgnoredGame ig WHERE ig.uniqueId IS NOT NULL", String.class).list();
        }
    }

    /**
     * Get all ignored normalized titles.
     */
    public List<String> findAllNormalizedTitles() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT ig.normalizedTitle FROM IgnoredGame ig WHERE ig.normalizedTitle IS NOT NULL", String.class).list();
        }
    }

    /**
     * Get all ignored install paths.
     */
    public List<String> findAllInstallPaths() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT ig.installPath FROM IgnoredGame ig WHERE ig.installPath IS NOT NULL", String.class).list();
        }
    }

    /**
     * Check if a game is ignored by normalized title.
     */
    public boolean isIgnoredByNormalizedTitle(String normalizedTitle) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("SELECT COUNT(ig) FROM IgnoredGame ig WHERE ig.normalizedTitle = :title", Long.class)
                    .setParameter("title", normalizedTitle)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    /**
     * Check if a game is ignored by install path.
     */
    public boolean isIgnoredByInstallPath(String installPath) {
        if (installPath == null || installPath.isEmpty()) return false;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("SELECT COUNT(ig) FROM IgnoredGame ig WHERE ig.installPath = :path", Long.class)
                    .setParameter("path", installPath)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    /**
     * Delete an ignored game by ID.
     */
    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            IgnoredGame ignoredGame = session.get(IgnoredGame.class, id);
            if (ignoredGame != null) {
                session.remove(ignoredGame);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Delete an ignored game entity.
     */
    public void delete(IgnoredGame ignoredGame) {
        if (ignoredGame != null && ignoredGame.getId() != null) {
            delete(ignoredGame.getId());
        }
    }

    /**
     * Delete an ignored game by unique ID.
     */
    public void deleteByUniqueId(String uniqueId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.createMutationQuery("DELETE FROM IgnoredGame WHERE uniqueId = :uid")
                    .setParameter("uid", uniqueId)
                    .executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Count all ignored games.
     */
    public long count() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT COUNT(ig) FROM IgnoredGame ig", Long.class).uniqueResult();
        }
    }

    /**
     * Migrates existing ignored games to populate the normalizedTitle field.
     * Call this once on startup to ensure all records have the field populated.
     */
    public void migrateNormalizedTitles() {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            List<IgnoredGame> gamesWithoutNormalizedTitle = session
                .createQuery("FROM IgnoredGame WHERE normalizedTitle IS NULL OR normalizedTitle = ''", IgnoredGame.class)
                .list();

            for (IgnoredGame ig : gamesWithoutNormalizedTitle) {
                if (ig.getTitle() != null) {
                    ig.setNormalizedTitle(com.nexus.model.IgnoredGame.normalizeTitle(ig.getTitle()));
                    session.merge(ig);
                    System.out.println("[IgnoredGameRepository] Migrated normalizedTitle for: " + ig.getTitle());
                }
            }

            transaction.commit();
            System.out.println("[IgnoredGameRepository] Migration complete. Updated " + gamesWithoutNormalizedTitle.size() + " records.");
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            System.err.println("[IgnoredGameRepository] Migration failed: " + e.getMessage());
        }
    }
}

