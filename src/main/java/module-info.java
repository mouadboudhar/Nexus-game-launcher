module com.nexus.launcher {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires java.sql;
    requires java.naming;
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.core;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    // Model package - open to Hibernate for entity mapping
    opens com.nexus.model to org.hibernate.orm.core, javafx.fxml;

    // Controller and UI packages - open to JavaFX
    opens com.nexus.controller to javafx.fxml;
    opens com.nexus.component to javafx.fxml;
    opens com.nexus.service to javafx.fxml;
    opens com.nexus.util to javafx.fxml;

    // Exports
    exports com.nexus.model;
    exports com.nexus.repository;
    exports com.nexus.util;
    exports com.nexus.controller;
    exports com.nexus.component;
    exports com.nexus.service;
    exports com.nexus;
    opens com.nexus to javafx.fxml;
}

