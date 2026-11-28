package org.sequeless.ontology.postgres;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.postgresql.ds.PGSimpleDataSource;
import org.sequeless.ontology.testfixtures.OntologyDocumentStoreContract;
import org.sequeless.store.postgres.PostgresMigrations;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves {@link PostgresOntologyDocumentStore} satisfies {@link OntologyDocumentStoreContract}, the
 * released document-store contract, against a real PostgreSQL 18 instance started by Testcontainers.
 *
 * <p>The schema is the store adapter's: {@link #migrate()} runs {@link PostgresMigrations} from
 * {@code store-postgres-jsonb-adapter} once per container, so {@code sq_ontology_document} is the published
 * table contract (spec 18 Q2). {@link #freshDocuments()} returns a store over the container's
 * {@link DataSource} on every call, and the table is truncated after each test, because the contract
 * requires a brand-new, empty store.
 */
@Testcontainers
class PostgresOntologyDocumentStoreContractIT extends OntologyDocumentStoreContract {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    private static DataSource dataSource;

    @BeforeAll
    static void migrate() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUser(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        dataSource = ds;
        PostgresMigrations.migrate(dataSource);
    }

    @AfterEach
    void truncateTables() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("TRUNCATE TABLE sq_ontology_document");
        }
    }

    @Override
    protected PostgresOntologyDocumentStore freshDocuments() {
        return new PostgresOntologyDocumentStore(dataSource);
    }
}
