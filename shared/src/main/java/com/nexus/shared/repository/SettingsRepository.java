package com.nexus.shared.repository;

import com.nexus.shared.model.AppSettings;
import com.nexus.shared.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Repository for AppSettings entity operations using Hibernate Native.
 */
public class SettingsRepository {

    /**
     * Get the app settings (creates default if not exists).
     */
    public AppSettings getSettings() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            AppSettings settings = session.createQuery("FROM AppSettings", AppSettings.class)
                    .setMaxResults(1)
                    .uniqueResult();

            if (settings == null) {
                settings = new AppSettings();
                settings.setDarkMode(true);
                settings.setLaunchOnStartup(false);
                settings.setCloseToTray(false);
                save(settings);
            }
            return settings;
        }
    }

    /**
     * Save or update app settings.
     */
    public AppSettings save(AppSettings settings) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            if (settings.getId() == null) {
                session.persist(settings);
            } else {
                settings = session.merge(settings);
            }
            transaction.commit();
            return settings;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Update a specific setting.
     */
    public void updateSetting(String settingName, Object value) {
        AppSettings settings = getSettings();
        switch (settingName) {
            case "launchOnStartup" -> settings.setLaunchOnStartup((Boolean) value);
            case "closeToTray" -> settings.setCloseToTray((Boolean) value);
            case "darkMode" -> settings.setDarkMode((Boolean) value);
            case "steamLibraryPath" -> settings.setSteamLibraryPath((String) value);
            case "epicGamesPath" -> settings.setEpicGamesPath((String) value);
        }
        save(settings);
    }
}
