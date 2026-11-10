package org.sequeless.ontology.testfixtures;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.kernel.identity.TenantId;
import org.sequeless.ontology.api.ImportMode;
import org.sequeless.ontology.api.ImportReport;
import org.sequeless.ontology.api.OntologyAdministration;
import org.sequeless.ontology.api.OntologyDocument;
import org.sequeless.ontology.api.OntologyException;
import org.sequeless.ontology.api.OntologyFormat;
import org.sequeless.ontology.api.OntologyIssue;
import org.sequeless.ontology.api.OntologyReport;
import org.sequeless.ontology.api.Severity;
import org.sequeless.ontology.automation.api.Automation;
import org.sequeless.ontology.automation.spi.AutomationSource;
import org.sequeless.ontology.behaviour.api.Behaviour;
import org.sequeless.ontology.behaviour.spi.BehaviourSource;
import org.sequeless.ontology.schema.api.DisplayHints;
import org.sequeless.ontology.schema.api.OntologyVersion;
import org.sequeless.ontology.schema.api.Schema;
import org.sequeless.ontology.schema.api.TypeDefinition;
import org.sequeless.ontology.schema.spi.SchemaSource;

/**
 * Holds the current backing document and a version counter, and rebuilds the three halves from
 * the document on every read — no caching, no parsing, no reasoning of any kind. The counter
 * starts at 1 and goes up by one on each successful reload or import. A failed reload or import
 * leaves both the document and the counter as they were.
 */
public final class InMemoryOntologyPort
        implements SchemaSource, BehaviourSource, AutomationSource, OntologyAdministration {

    private static final String FAKE_ONTOLOGY_IRI = "https://example.org/fake";
    private static final List<TypeDefinition> TYPES = List.of(new TypeDefinition(
            "https://example.org/fake#Thing", "Thing", List.of(), List.of(), DisplayHints.none(), false));

    private OntologyDocument source;
    private int version = 1;

    public InMemoryOntologyPort(OntologyDocument document) {
        this.source = Objects.requireNonNull(document, "document must not be null");
    }

    @Override
    public Schema schema(TenantId tenant) {
        Objects.requireNonNull(tenant, "tenant must not be null");
        requireConsistent(source);
        return new Schema(new OntologyVersion(version), FAKE_ONTOLOGY_IRI, Optional.empty(), Map.of(), TYPES);
    }

    @Override
    public Behaviour behaviour(TenantId tenant) {
        Objects.requireNonNull(tenant, "tenant must not be null");
        requireConsistent(source);
        return new Behaviour(new OntologyVersion(version), Map.of(), Map.of());
    }

    @Override
    public Automation automation(TenantId tenant) {
        Objects.requireNonNull(tenant, "tenant must not be null");
        requireConsistent(source);
        return new Automation(new OntologyVersion(version), Map.of());
    }

    @Override
    public OntologyReport reload(Scope scope) {
        Objects.requireNonNull(scope, "scope must not be null");
        requireConsistent(source);
        version++;
        return new OntologyReport(true, List.of());
    }

    @Override
    public OntologyDocument export(Scope scope, OntologyFormat format) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(format, "format must not be null");
        requireConsistent(source);
        return new OntologyDocument(source.content(), format);
    }

    @Override
    public ImportReport importDocument(Scope scope, OntologyDocument document, ImportMode mode) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(document, "document must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        requireConsistent(document);
        this.source = document;
        version++;
        return new ImportReport(true, new OntologyReport(true, List.of()), TYPES.size());
    }

    /**
     * The one piece of genuine branching in this fake: a document whose content matches {@link
     * Fixtures#inconsistentOntology()} is rejected, everything else is accepted and mapped to the
     * same fixed shape regardless of what it actually contains.
     */
    private static void requireConsistent(OntologyDocument document) {
        if (document.content().equals(Fixtures.inconsistentOntology().content())) {
            OntologyReport report = new OntologyReport(
                    false,
                    List.of(new OntologyIssue(
                            Severity.ERROR,
                            Optional.empty(),
                            "fake port: document matches the known-inconsistent fixture")));
            throw new OntologyException(report);
        }
    }
}
