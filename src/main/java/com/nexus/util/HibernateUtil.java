package com.nexus.util;

import com.nexus.model.Game;
import com.nexus.model.AppSettings;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Hibernate utility class for managing SessionFactory.
 * Uses hibernate.cfg.xml for configuration.
 */
public class HibernateUtil {

    private static SessionFactory sessionFactory;
    private static final Object lock = new Object();

    private static void buildSessionFactory() {
        try {
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");

            // Explicitly add annotated classes
            configuration.addAnnotatedClass(Game.class);
            configuration.addAnnotatedClass(AppSettings.class);

            sessionFactory = configuration.buildSessionFactory();
            System.out.println("[HibernateUtil] SessionFactory created successfully");

        } catch (Throwable ex) {
            System.err.println("[HibernateUtil] SessionFactory creation failed: " + ex.getMessage());
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (lock) {
                if (sessionFactory == null) {
                    buildSessionFactory();
                }
            }
        }
        return sessionFactory;
    }

    /**
     * Opens a new session from the factory.
     */
    public static Session openSession() {
        return getSessionFactory().openSession();
    }

    /**
     * Initializes the database (creates tables if they don't exist).
     */
    public static void initialize() {
        try (Session session = openSession()) {
            System.out.println("[HibernateUtil] Database initialized successfully");
        } catch (Exception e) {
            System.err.println("[HibernateUtil] Database initialization failed: " + e.getMessage());
            throw e;
        }
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            System.out.println("[HibernateUtil] SessionFactory closed");
        }
    }
}

