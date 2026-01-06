package com.nexus.repository;

import com.nexus.model.Game;
import com.nexus.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Game entity CRUD operations using Hibernate Native.
 */
public class GameRepository {

    /**
     * Save or update a game.
     */
    public Game save(Game game) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            if (game.getId() == null) {
                session.persist(game);
            } else {
                game = session.merge(game);
            }
            transaction.commit();
            return game;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Find a game by ID.
     */
    public Optional<Game> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Game.class, id));
        }
    }

    /**
     * Get all games.
     */
    public List<Game> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Game ORDER BY title", Game.class).list();
        }
    }

    /**
     * Find games by favorite status.
     */
    public List<Game> findByFavorite(boolean favorite) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Game WHERE favorite = :fav ORDER BY title", Game.class)
                    .setParameter("fav", favorite)
                    .list();
        }
    }

    /**
     * Search games by title (case-insensitive).
     */
    public List<Game> searchByTitle(String keyword) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Game WHERE LOWER(title) LIKE LOWER(:kw) ORDER BY title", Game.class)
                    .setParameter("kw", "%" + keyword + "%")
                    .list();
        }
    }

    /**
     * Find games by platform.
     */
    public List<Game> findByPlatform(Game.Platform platform) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Game WHERE platform = :platform ORDER BY title", Game.class)
                    .setParameter("platform", platform)
                    .list();
        }
    }

    /**
     * Delete a game by ID.
     */
    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Game game = session.get(Game.class, id);
            if (game != null) {
                session.remove(game);
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
     * Delete a game entity.
     */
    public void delete(Game game) {
        if (game != null && game.getId() != null) {
            delete(game.getId());
        }
    }

    /**
     * Count all games.
     */
    public long count() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT COUNT(g) FROM Game g", Long.class).uniqueResult();
        }
    }

    /**
     * Update favorite status of a game.
     */
    public void updateFavorite(Long id, boolean favorite) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Game game = session.get(Game.class, id);
            if (game != null) {
                game.setFavorite(favorite);
                session.merge(game);
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
     * Find a game by its unique ID.
     */
    public Optional<Game> findByUniqueId(String uniqueId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Game WHERE uniqueId = :uid", Game.class)
                    .setParameter("uid", uniqueId)
                    .uniqueResultOptional();
        }
    }

    /**
     * Save or update multiple games in a batch.
     */
    public List<Game> saveAll(List<Game> games) {
        Transaction transaction = null;
        List<Game> savedGames = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            for (Game game : games) {
                if (game.getId() == null) {
                    session.persist(game);
                } else {
                    game = session.merge(game);
                }
                savedGames.add(game);
            }
            transaction.commit();
            return savedGames;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Get all unique IDs in the database.
     */
    public List<String> findAllUniqueIds() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT g.uniqueId FROM Game g WHERE g.uniqueId IS NOT NULL", String.class).list();
        }
    }

    /**
     * Delete all games not in the given list of unique IDs.
     */
    public int deleteNotIn(List<String> uniqueIds) {
        if (uniqueIds == null || uniqueIds.isEmpty()) {
            return 0;
        }
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            int deleted = session.createMutationQuery("DELETE FROM Game WHERE uniqueId NOT IN :ids AND platform != 'MANUAL'")
                    .setParameter("ids", uniqueIds)
                    .executeUpdate();
            transaction.commit();
            return deleted;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Delete all games from the database.
     */
    public int deleteAll() {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            int deleted = session.createMutationQuery("DELETE FROM Game").executeUpdate();
            transaction.commit();
            return deleted;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
}

