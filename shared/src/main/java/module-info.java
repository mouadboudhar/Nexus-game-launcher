module com.nexus.shared {
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires java.naming;

    exports com.nexus.shared.model;
    exports com.nexus.shared.util;
    exports com.nexus.shared.repository;

    opens com.nexus.shared.model to org.hibernate.orm.core;
}

