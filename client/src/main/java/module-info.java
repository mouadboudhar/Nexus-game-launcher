module com.nexus.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.nexus.shared;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires java.sql;

    opens com.nexus.client to javafx.fxml;
    opens com.nexus.client.controller to javafx.fxml;
    opens com.nexus.client.component to javafx.fxml;
    opens com.nexus.client.service to javafx.fxml;

    exports com.nexus.client;
    exports com.nexus.client.controller;
    exports com.nexus.client.component;
    exports com.nexus.client.service;
}

