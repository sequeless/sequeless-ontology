package org.sequeless.ontology.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.ontology.api.OntologyDocument;
import org.sequeless.ontology.api.OntologyFormat;
import org.sequeless.ontology.spi.OntologyDocumentStore;
import org.sequeless.ontology.spi.StoredOntologyDocument;

/**
 * The PostgreSQL-backed {@link OntologyDocumentStore}, over the {@code sq_ontology_document} table.
 *
 * <p>The table is created by the store adapter's migrations ({@code store-postgres-jsonb-adapter}, V1),
 * which this module does not ship. Plain JDBC: each call takes a connection from the {@link DataSource},
 * and {@link #activate} runs in one transaction on that connection.
 *
 * <p>Unlike a Spring {@code TransactionTemplate}, this store does not join an ambient transaction. Each
 * call commits or rolls back on its own connection, so it is not undone by an outer transaction's rollback.
 */
public final class PostgresOntologyDocumentStore implements OntologyDocumentStore {

    private final DataSource dataSource;

    /**
     * @param dataSource the DataSource that reaches a database migrated to the store's schema
     */
    public PostgresOntologyDocumentStore(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<StoredOntologyDocument> active(Scope scope) {
        Objects.requireNonNull(scope, "scope must not be null");
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, version, format, content, created_at FROM sq_ontology_document "
                                + "WHERE tenant_id = ? AND active")) {
            statement.setString(1, scope.tenantId().value());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                StoredOntologyDocument active = mapRow(rs);
                if (rs.next()) {
                    throw new IllegalStateException("more than one active ontology document for tenant");
                }
                return Optional.of(active);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("could not read the active ontology document", e);
        }
    }

    @Override
    public StoredOntologyDocument activate(Scope scope, OntologyDocument document) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(document, "document must not be null");
        String tenantId = scope.tenantId().value();

        return inTransaction(connection -> {
            // An advisory lock scoped to the tenant serializes concurrent activate() calls for the
            // same tenant across the deactivate/read-max/insert sequence below; PostgreSQL's default
            // READ COMMITTED isolation does not otherwise prevent two concurrent transactions from
            // computing the same "next version" for the same tenant.
            try (PreparedStatement lock = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
                lock.setString(1, tenantId);
                try (ResultSet ignored = lock.executeQuery()) {
                    ignored.next();
                }
            }

            try (PreparedStatement deactivate = connection.prepareStatement(
                    "UPDATE sq_ontology_document SET active = false WHERE tenant_id = ? AND active")) {
                deactivate.setString(1, tenantId);
                deactivate.executeUpdate();
            }

            int nextVersion;
            try (PreparedStatement max = connection.prepareStatement(
                    "SELECT COALESCE(MAX(version), 0) + 1 FROM sq_ontology_document WHERE tenant_id = ?")) {
                max.setString(1, tenantId);
                try (ResultSet rs = max.executeQuery()) {
                    rs.next();
                    nextVersion = rs.getInt(1);
                }
            }

            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO sq_ontology_document (id, tenant_id, version, format, content, active, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, true, ?)")) {
                insert.setObject(1, id);
                insert.setString(2, tenantId);
                insert.setInt(3, nextVersion);
                insert.setString(4, document.format().name());
                insert.setString(5, document.content());
                insert.setTimestamp(6, Timestamp.from(createdAt));
                insert.executeUpdate();
            }

            return new StoredOntologyDocument(id, nextVersion, document, createdAt);
        });
    }

    private static StoredOntologyDocument mapRow(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        int version = rs.getInt("version");
        OntologyFormat format = OntologyFormat.valueOf(rs.getString("format"));
        String content = rs.getString("content");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        return new StoredOntologyDocument(id, version, new OntologyDocument(content, format), createdAt);
    }

    /** Work that runs on one connection inside a transaction. */
    @FunctionalInterface
    private interface TransactionalWork<T> {
        T apply(Connection connection) throws SQLException;
    }

    /**
     * Runs {@code work} on a fresh connection in one transaction: commits on success and rolls back on any
     * failure. Auto-commit is not restored before close: the connection goes back to its pool with auto-commit
     * off, and the pool resets it. Restoring it after a failed rollback would make the driver commit partial work.
     */
    private <T> T inTransaction(TransactionalWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException | Error failure) {
                rollbackQuietly(connection, failure);
                throw failure;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("could not activate the ontology document", e);
        }
    }

    private static void rollbackQuietly(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
