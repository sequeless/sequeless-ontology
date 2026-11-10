package org.sequeless.ontology.testfixtures;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.kernel.identity.TenantId;
import org.sequeless.ontology.api.OntologyDocument;
import org.sequeless.ontology.spi.OntologyDocumentStore;
import org.sequeless.ontology.spi.StoredOntologyDocument;

/**
 * A per-tenant version counter with no persistence at all: {@link #activate} assigns the next
 * integer for the tenant and overwrites whatever was previously active, which is all {@link
 * OntologyDocumentStore}'s contract requires — there is no separate "deactivate" step to forget
 * because only the currently-active document is ever kept.
 */
public final class InMemoryOntologyDocumentStore implements OntologyDocumentStore {

    private final Object lock = new Object();
    private final Map<TenantId, StoredOntologyDocument> active = new LinkedHashMap<>();
    private final Map<TenantId, Integer> nextVersion = new LinkedHashMap<>();

    @Override
    public Optional<StoredOntologyDocument> active(Scope scope) {
        Objects.requireNonNull(scope, "scope must not be null");
        synchronized (lock) {
            return Optional.ofNullable(active.get(scope.tenantId()));
        }
    }

    @Override
    public StoredOntologyDocument activate(Scope scope, OntologyDocument document) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(document, "document must not be null");
        synchronized (lock) {
            int version = nextVersion.merge(scope.tenantId(), 1, Integer::sum);
            StoredOntologyDocument stored =
                    new StoredOntologyDocument(UUID.randomUUID(), version, document, Instant.now());
            active.put(scope.tenantId(), stored);
            return stored;
        }
    }
}
