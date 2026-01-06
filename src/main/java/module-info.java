module com.nexus.launcher {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires java.sql;
    requires java.naming;

    // Model package - open to Hibernate for entity mapping
    opens com.nexus.model to org.hibernate.orm.core, javafx.fxml;

    // Controller and UI packages - open to JavaFX
    opens com.nexus.client to javafx.fxml;
    opens com.nexus.client.controller to javafx.fxml;
    opens com.nexus.client.component to javafx.fxml;
    opens com.nexus.client.service to javafx.fxml;
    opens com.nexus.client.util to javafx.fxml;

    // Exports
    exports com.nexus.model;
    exports com.nexus.repository;
    exports com.nexus.util;
    exports com.nexus.client;
    exports com.nexus.client.controller;
    exports com.nexus.client.component;
    exports com.nexus.client.service;
    exports com.nexus.client.util;
}

