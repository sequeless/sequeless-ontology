package org.sequeless.ontology.facet.core.testfixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.sequeless.ontology.facet.core.api.Capabilities;
import org.sequeless.ontology.facet.core.api.Cardinality;
import org.sequeless.ontology.facet.core.api.FacetType;
import org.sequeless.ontology.facet.core.api.InferenceSupport;
import org.sequeless.ontology.facet.core.api.OperatorRestriction;

/**
 * Nine canned {@link ContractFixture} scenarios, each exercising one facet of design.md's
 * semantics: version merge (§4.4), merge conflicts (§4.5), the traversal guards (§4.3), and RBAC
 * facet filtering (§4.10).
 *
 * <p>Every scenario belongs to tenant {@code "acme"}. Version IRIs are {@code "urn:ont:v1"},
 * {@code "urn:ont:v2"}, and (reserved for scenarios needing a third version) {@code
 * "urn:ont:v3"}. Every identity below — every distinct property, relationship, and class across
 * every scenario — is a fixed {@link UUID} constant, so a test can assert against a specific
 * identity without re-deriving it from a fixture instance. Where the same conceptual property
 * (for example {@code Invoice.status} or {@code Invoice.total}) appears unchanged in more than one
 * scenario, it deliberately reuses the same UUID constant across those scenarios.
 */
public final class ContractFixtures {

    private static final String TENANT = "acme";
    private static final String V1 = "urn:ont:v1";
    private static final String V2 = "urn:ont:v2";
    // Reserved for future scenarios that need a third version in scope; none of the nine below do.
    private static final String V3 = "urn:ont:v3";

    // --- Classes -----------------------------------------------------------------------------
    private static final UUID INVOICE_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID AUDIT_LOG_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID A_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID B_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID C_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID LEVEL0_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final UUID LEVEL1_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID LEVEL2_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID LEVEL3_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID LEVEL4_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID LEVEL5_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-00000000000d");

    // --- Properties ----------------------------------------------------------------------------
    private static final UUID STATUS_ID = UUID.fromString("00000000-0000-0000-0000-00000000000e");
    private static final UUID TOTAL_ID = UUID.fromString("00000000-0000-0000-0000-00000000000f");
    private static final UUID DESCRIPTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID INTERNAL_NOTES_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID CUSTOMER_NAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID REGION_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID ISSUED_AT_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
    private static final UUID LEGACY_FIELD_ID = UUID.fromString("00000000-0000-0000-0000-000000000015");
    private static final UUID NEW_FIELD_ID = UUID.fromString("00000000-0000-0000-0000-000000000016");
    private static final UUID NAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000017");
    private static final UUID CITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000018");
    private static final UUID TIER_ID = UUID.fromString("00000000-0000-0000-0000-000000000019");
    private static final UUID REFERENCE_ID = UUID.fromString("00000000-0000-0000-0000-00000000001a");
    private static final UUID NOTE_ID = UUID.fromString("00000000-0000-0000-0000-00000000001b");
    private static final UUID A_NAME_ID = UUID.fromString("00000000-0000-0000-0000-00000000001c");
    private static final UUID B_NAME_ID = UUID.fromString("00000000-0000-0000-0000-00000000001d");
    private static final UUID C_NAME_ID = UUID.fromString("00000000-0000-0000-0000-00000000001e");
    private static final UUID LEVEL0_VALUE_ID = UUID.fromString("00000000-0000-0000-0000-00000000001f");
    private static final UUID LEVEL1_VALUE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID LEVEL2_VALUE_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID LEVEL3_VALUE_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID LEVEL4_VALUE_ID = UUID.fromString("00000000-0000-0000-0000-000000000023");
    private static final UUID LEVEL5_VALUE_ID = UUID.fromString("00000000-0000-0000-0000-000000000024");

    // --- Relationships -------------------------------------------------------------------------
    private static final UUID INVOICE_CUSTOMER_REL_ID = UUID.fromString("00000000-0000-0000-0000-000000000025");
    private static final UUID INVOICE_INTERNAL_AUDIT_REL_ID = UUID.fromString("00000000-0000-0000-0000-000000000026");
    private static final UUID CUSTOMER_ACCOUNT_REL_ID = UUID.fromString("00000000-0000-0000-0000-000000000027");
    private static final UUID A_TO_B_REL_ID = UUID.fromString("00000000-0000-0000-0000-000000000028");
    private static final UUID A_TO_C_REL_ID = UUID.fromString("00000000-0000-0000-0000-000000000029");
    private static final UUID B_TO_A_REL_ID = UUID.fromString("00000000-0000-0000-0000-00000000002a");
    private static final UUID C_TO_B_REL_ID = UUID.fromString("00000000-0000-0000-0000-00000000002b");
    private static final UUID LEVEL0_STEP_REL_ID = UUID.fromString("00000000-0000-0000-0000-00000000002c");
    private static final UUID LEVEL1_STEP_REL_ID = UUID.fromString("00000000-0000-0000-0000-00000000002d");
    private static final UUID LEVEL2_STEP_REL_ID = UUID.fromString("00000000-0000-0000-0000-00000000002e");
    private static final UUID LEVEL3_STEP_REL_ID = UUID.fromString("00000000-0000-0000-0000-00000000002f");
    private static final UUID LEVEL4_STEP_REL_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");

    // --- Capability shapes -----------------------------------------------------------------------
    private static final Capabilities FILTERABLE_ONLY = new Capabilities(true, false, false, false);
    private static final Capabilities FILTERABLE_FACETABLE = new Capabilities(true, true, false, false);
    private static final Capabilities FILTERABLE_FACETABLE_SORTABLE = new Capabilities(true, true, false, true);
    private static final Capabilities FILTERABLE_SORTABLE = new Capabilities(true, false, false, true);
    private static final Capabilities FILTERABLE_SEARCHABLE = new Capabilities(true, false, true, false);
    private static final Capabilities NO_CAPABILITIES = new Capabilities(false, false, false, false);

    private ContractFixtures() {}

    /**
     * Invoice (root), Customer, Account, AuditLog — a small acyclic graph exercising ordinary
     * scalar properties, a walkable relationship, and an unwalkable one, all in v1 only.
     *
     * @return the {@code invoiceAndCustomer} scenario
     */
    public static ContractFixture invoiceAndCustomer() {
        ClassFixture invoice = new ClassFixture(
                INVOICE_CLASS_ID,
                "Invoice",
                classIri("Invoice"),
                Set.of(),
                List.of(
                        scalarProperty(STATUS_ID, "status", FacetType.STRING, FILTERABLE_FACETABLE_SORTABLE, V1),
                        scalarProperty(TOTAL_ID, "total", FacetType.DECIMAL, FILTERABLE_SORTABLE, V1),
                        scalarProperty(DESCRIPTION_ID, "description", FacetType.STRING, FILTERABLE_SEARCHABLE, V1),
                        scalarProperty(INTERNAL_NOTES_ID, "internalNotes", FacetType.STRING, NO_CAPABILITIES, V1)),
                List.of(
                        relationship(INVOICE_CUSTOMER_REL_ID, "customer", "Customer", true, V1),
                        relationship(INVOICE_INTERNAL_AUDIT_REL_ID, "internalAudit", "AuditLog", false, V1)));

        ClassFixture customer = new ClassFixture(
                CUSTOMER_CLASS_ID,
                "Customer",
                classIri("Customer"),
                Set.of(),
                List.of(
                        scalarProperty(NAME_ID, "name", FacetType.STRING, FILTERABLE_ONLY, V1),
                        scalarProperty(CITY_ID, "city", FacetType.STRING, FILTERABLE_FACETABLE, V1),
                        scalarProperty(TIER_ID, "tier", FacetType.STRING, FILTERABLE_FACETABLE, V1)),
                List.of(relationship(CUSTOMER_ACCOUNT_REL_ID, "account", "Account", true, V1)));

        ClassFixture account = new ClassFixture(
                ACCOUNT_CLASS_ID,
                "Account",
                classIri("Account"),
                Set.of(),
                List.of(scalarProperty(REFERENCE_ID, "reference", FacetType.STRING, FILTERABLE_ONLY, V1)),
                List.of());

        ClassFixture auditLog = new ClassFixture(
                AUDIT_LOG_CLASS_ID,
                "AuditLog",
                classIri("AuditLog"),
                Set.of(),
                List.of(scalarProperty(NOTE_ID, "note", FacetType.STRING, FILTERABLE_ONLY, V1)),
                List.of());

        return new ContractFixture(TENANT, List.of(V1), List.of(invoice, customer, account, auditLog));
    }

    /**
     * Invoice (root) only, v1+v2. {@code custName} is renamed to {@code customerName} between
     * versions (same property UUID); {@code total} is unchanged across both; {@code region} is
     * added in v2 only.
     *
     * @return the {@code renamedAcrossVersions} scenario
     */
    public static ContractFixture renamedAcrossVersions() {
        ClassFixture invoice = new ClassFixture(
                INVOICE_CLASS_ID,
                "Invoice",
                classIri("Invoice"),
                Set.of(),
                List.of(
                        scalarProperty(CUSTOMER_NAME_ID, "custName", FacetType.STRING, FILTERABLE_ONLY, V1),
                        scalarProperty(CUSTOMER_NAME_ID, "customerName", FacetType.STRING, FILTERABLE_ONLY, V2),
                        scalarProperty(TOTAL_ID, "total", FacetType.DECIMAL, FILTERABLE_ONLY, V1),
                        scalarProperty(TOTAL_ID, "total", FacetType.DECIMAL, FILTERABLE_ONLY, V2),
                        scalarProperty(REGION_ID, "region", FacetType.STRING, FILTERABLE_ONLY, V2)),
                List.of());

        return new ContractFixture(TENANT, List.of(V1, V2), List.of(invoice));
    }

    /**
     * Invoice (root) only, v1+v2. {@code issuedAt} is typed {@code STRING} in v1 and {@code
     * DATETIME} in v2 — a type conflict; {@code total} is an unconflicted sibling, unchanged
     * across both versions.
     *
     * @return the {@code conflictingTypes} scenario
     */
    public static ContractFixture conflictingTypes() {
        ClassFixture invoice = new ClassFixture(
                INVOICE_CLASS_ID,
                "Invoice",
                classIri("Invoice"),
                Set.of(),
                List.of(
                        scalarProperty(ISSUED_AT_ID, "issuedAt", FacetType.STRING, FILTERABLE_ONLY, V1),
                        scalarProperty(ISSUED_AT_ID, "issuedAt", FacetType.DATETIME, FILTERABLE_ONLY, V2),
                        scalarProperty(TOTAL_ID, "total", FacetType.DECIMAL, FILTERABLE_ONLY, V1),
                        scalarProperty(TOTAL_ID, "total", FacetType.DECIMAL, FILTERABLE_ONLY, V2)),
                List.of());

        return new ContractFixture(TENANT, List.of(V1, V2), List.of(invoice));
    }

    /**
     * Invoice (root) only, v1+v2. {@code status} and {@code total} are unchanged across both
     * versions; {@code region} exists only in v2 — the case a {@link
     * org.sequeless.ontology.facet.core.api.VersionPolicy.Intersection} policy excludes.
     *
     * @return the {@code versionExclusive} scenario
     */
    public static ContractFixture versionExclusive() {
        ClassFixture invoice = new ClassFixture(
                INVOICE_CLASS_ID,
                "Invoice",
                classIri("Invoice"),
                Set.of(),
                List.of(
                        scalarProperty(STATUS_ID, "status", FacetType.STRING, FILTERABLE_ONLY, V1),
                        scalarProperty(STATUS_ID, "status", FacetType.STRING, FILTERABLE_ONLY, V2),
                        scalarProperty(TOTAL_ID, "total", FacetType.DECIMAL, FILTERABLE_ONLY, V1),
                        scalarProperty(TOTAL_ID, "total", FacetType.DECIMAL, FILTERABLE_ONLY, V2),
                        scalarProperty(REGION_ID, "region", FacetType.STRING, FILTERABLE_ONLY, V2)),
                List.of());

        return new ContractFixture(TENANT, List.of(V1, V2), List.of(invoice));
    }

    /**
     * Invoice (root) only, v1+v2, with zero shared property ids between the two versions:
     * {@code legacyField} exists only in v1, {@code newField} only in v2.
     *
     * @return the {@code disjointVersions} scenario
     */
    public static ContractFixture disjointVersions() {
        ClassFixture invoice = new ClassFixture(
                INVOICE_CLASS_ID,
                "Invoice",
                classIri("Invoice"),
                Set.of(),
                List.of(
                        scalarProperty(LEGACY_FIELD_ID, "legacyField", FacetType.STRING, FILTERABLE_ONLY, V1),
                        scalarProperty(NEW_FIELD_ID, "newField", FacetType.STRING, FILTERABLE_ONLY, V2)),
                List.of());

        return new ContractFixture(TENANT, List.of(V1, V2), List.of(invoice));
    }

    /**
     * Invoice (root), Customer, v1 only. The {@code customer} relationship exists but is marked
     * unwalkable — exercising traversal guard 1, {@code EDGE_NOT_WALKABLE} (design.md section
     * 4.3).
     *
     * @return the {@code unwalkableEdge} scenario
     */
    public static ContractFixture unwalkableEdge() {
        ClassFixture invoice = new ClassFixture(
                INVOICE_CLASS_ID,
                "Invoice",
                classIri("Invoice"),
                Set.of(),
                List.of(scalarProperty(STATUS_ID, "status", FacetType.STRING, FILTERABLE_ONLY, V1)),
                List.of(relationship(INVOICE_CUSTOMER_REL_ID, "customer", "Customer", false, V1)));

        ClassFixture customer = new ClassFixture(
                CUSTOMER_CLASS_ID,
                "Customer",
                classIri("Customer"),
                Set.of(),
                List.of(scalarProperty(CITY_ID, "city", FacetType.STRING, FILTERABLE_ONLY, V1)),
                List.of());

        return new ContractFixture(TENANT, List.of(V1), List.of(invoice, customer));
    }

    /**
     * A (root), B, C, v1 only, all relationships walkable: {@code A.toB}, {@code A.toC}, {@code
     * B.toA} (closing the cycle back to root), and {@code C.toB}. Exercises traversal guard 2,
     * {@code CYCLE_DETECTED} (design.md section 4.3).
     *
     * @return the {@code cyclic} scenario
     */
    public static ContractFixture cyclic() {
        ClassFixture a = new ClassFixture(
                A_CLASS_ID,
                "A",
                classIri("A"),
                Set.of(),
                List.of(scalarProperty(A_NAME_ID, "name", FacetType.STRING, FILTERABLE_ONLY, V1)),
                List.of(
                        relationship(A_TO_B_REL_ID, "toB", "B", true, V1),
                        relationship(A_TO_C_REL_ID, "toC", "C", true, V1)));

        ClassFixture b = new ClassFixture(
                B_CLASS_ID,
                "B",
                classIri("B"),
                Set.of(),
                List.of(scalarProperty(B_NAME_ID, "name", FacetType.STRING, FILTERABLE_ONLY, V1)),
                List.of(relationship(B_TO_A_REL_ID, "toA", "A", true, V1)));

        // Deliberately a different UUID than A.toB: identity is per-class, and reusing A's toB id
        // here would wrongly claim this is the same relationship property as A's.
        ClassFixture c = new ClassFixture(
                C_CLASS_ID,
                "C",
                classIri("C"),
                Set.of(),
                List.of(scalarProperty(C_NAME_ID, "name", FacetType.STRING, FILTERABLE_ONLY, V1)),
                List.of(relationship(C_TO_B_REL_ID, "toB", "B", true, V1)));

        return new ContractFixture(TENANT, List.of(V1), List.of(a, b, c));
    }

    /**
     * Level0 (root) through Level5, six classes chained by a walkable {@code step} relationship,
     * v1 only. Exercises traversal guard 3, {@code DEPTH_EXCEEDED} (design.md section 4.3),
     * against the default max depth of 3.
     *
     * @return the {@code deep} scenario
     */
    public static ContractFixture deep() {
        List<UUID> levelClassIds = List.of(
                LEVEL0_CLASS_ID, LEVEL1_CLASS_ID, LEVEL2_CLASS_ID, LEVEL3_CLASS_ID, LEVEL4_CLASS_ID, LEVEL5_CLASS_ID);
        List<UUID> valueIds = List.of(
                LEVEL0_VALUE_ID, LEVEL1_VALUE_ID, LEVEL2_VALUE_ID, LEVEL3_VALUE_ID, LEVEL4_VALUE_ID, LEVEL5_VALUE_ID);
        List<UUID> stepRelIds = List.of(
                LEVEL0_STEP_REL_ID, LEVEL1_STEP_REL_ID, LEVEL2_STEP_REL_ID, LEVEL3_STEP_REL_ID, LEVEL4_STEP_REL_ID);

        List<ClassFixture> levels = new ArrayList<>();
        for (int level = 0; level <= 5; level++) {
            String slug = "Level" + level;
            List<RelationshipFixture> relationships = level < 5
                    ? List.of(relationship(stepRelIds.get(level), "step", "Level" + (level + 1), true, V1))
                    : List.of();
            levels.add(new ClassFixture(
                    levelClassIds.get(level),
                    slug,
                    classIri(slug),
                    Set.of(),
                    List.of(scalarProperty(valueIds.get(level), "value", FacetType.STRING, FILTERABLE_ONLY, V1)),
                    relationships));
        }

        return new ContractFixture(TENANT, List.of(V1), levels);
    }

    /**
     * Invoice (root, readable by every role) and Customer (readable only by {@code "finance"}),
     * v1 only. Exercises RBAC facet filtering (design.md section 4.10).
     *
     * @return the {@code rbacRestricted} scenario
     */
    public static ContractFixture rbacRestricted() {
        ClassFixture invoice = new ClassFixture(
                INVOICE_CLASS_ID,
                "Invoice",
                classIri("Invoice"),
                Set.of(),
                List.of(scalarProperty(STATUS_ID, "status", FacetType.STRING, FILTERABLE_ONLY, V1)),
                List.of(relationship(INVOICE_CUSTOMER_REL_ID, "customer", "Customer", true, V1)));

        ClassFixture customer = new ClassFixture(
                CUSTOMER_CLASS_ID,
                "Customer",
                classIri("Customer"),
                Set.of("finance"),
                List.of(scalarProperty(CITY_ID, "city", FacetType.STRING, FILTERABLE_ONLY, V1)),
                List.of());

        return new ContractFixture(TENANT, List.of(V1), List.of(invoice, customer));
    }

    private static String classIri(String slug) {
        return "urn:ont:class:" + slug;
    }

    private static PropertyFixture scalarProperty(
            UUID id, String slug, FacetType type, Capabilities capabilities, String version) {
        return new PropertyFixture(
                id,
                slug,
                type,
                Cardinality.SINGLE,
                capabilities,
                null,
                Optional.empty(),
                OperatorRestriction.unrestricted(),
                InferenceSupport.ASSERTED_ONLY,
                version);
    }

    private static RelationshipFixture relationship(
            UUID id, String slug, String targetClassSlug, boolean walkable, String... versions) {
        return new RelationshipFixture(id, slug, targetClassSlug, Cardinality.SINGLE, walkable, Set.of(versions));
    }
}
