package org.sequeless.ontology.facet.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.sequeless.ontology.facet.core.api.Capabilities;
import org.sequeless.ontology.facet.core.api.Cardinality;
import org.sequeless.ontology.facet.core.api.ConceptSchemeRef;
import org.sequeless.ontology.facet.core.api.Facet;
import org.sequeless.ontology.facet.core.api.FacetErrorCode;
import org.sequeless.ontology.facet.core.api.FacetPath;
import org.sequeless.ontology.facet.core.api.FacetResolutionException;
import org.sequeless.ontology.facet.core.api.FacetType;
import org.sequeless.ontology.facet.core.api.InferenceSupport;
import org.sequeless.ontology.facet.core.api.LocalizedText;
import org.sequeless.ontology.facet.core.api.OperatorRestriction;
import org.sequeless.ontology.facet.core.api.PathSegment;
import org.sequeless.ontology.facet.core.api.VersionPolicy;

class FacetMergerTest {

    private static final UUID CUSTOMER_NAME_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID REGION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID ISSUED_AT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

    private static final String V1 = "urn:ont:v1";
    private static final String V2 = "urn:ont:v2";
    private static final String V3 = "urn:ont:v3";
    private static final String UNKNOWN_VERSION = "urn:ont:v9";

    private static final Capabilities ALL_TRUE = new Capabilities(true, true, true, true);

    private static LocalizedText label(String value) {
        return new LocalizedText(value, "en");
    }

    private static FacetPath path(UUID propertyId, String slug) {
        return new FacetPath(List.of(new PathSegment(propertyId, slug, null)));
    }

    private static Facet facetWithVersions(
            UUID propertyId, String slug, FacetType type, Cardinality cardinality, Set<String> versions) {
        return new Facet(
                path(propertyId, slug),
                type,
                null,
                cardinality,
                ALL_TRUE,
                label(slug),
                null,
                Optional.empty(),
                OperatorRestriction.unrestricted(),
                InferenceSupport.ASSERTED_ONLY,
                versions,
                Optional.empty());
    }

    private static Facet facet(UUID propertyId, String slug, FacetType type, Cardinality cardinality, String version) {
        return facetWithVersions(propertyId, slug, type, cardinality, Set.of(version));
    }

    private static Facet facet(
            UUID propertyId,
            String slug,
            FacetType type,
            Cardinality cardinality,
            Capabilities capabilities,
            OperatorRestriction restriction,
            String nativeDatatypeIri,
            Optional<ConceptSchemeRef> conceptScheme,
            InferenceSupport inference,
            String version) {
        return new Facet(
                path(propertyId, slug),
                type,
                nativeDatatypeIri,
                cardinality,
                capabilities,
                label(slug),
                null,
                conceptScheme,
                restriction,
                inference,
                Set.of(version),
                Optional.empty());
    }

    @Test
    void unionMergesFacetSharingPropertyUuidAcrossVersions() {
        Facet v1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet v2 = facet(CUSTOMER_NAME_ID, "customerName", FacetType.STRING, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).definedInVersions()).containsExactlyInAnyOrder(V1, V2);
    }

    @Test
    void unionNewestVersionSlugWinsForDisplay() {
        Facet v1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet v2 = facet(CUSTOMER_NAME_ID, "customerName", FacetType.STRING, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged.get(0).path().asSlugPath()).isEqualTo("customerName");
        assertThat(merged.get(0).label().value()).isEqualTo("customerName");
    }

    @Test
    void unionRecordsUnionOfDefinedInVersions() {
        Facet v1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet v2 = facet(CUSTOMER_NAME_ID, "customerName", FacetType.STRING, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged.get(0).definedInVersions()).isEqualTo(Set.of(V1, V2));
    }

    @Test
    void unionKeepsVersionExclusiveFacet() {
        Facet customerNameV1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet customerNameV2 = facet(CUSTOMER_NAME_ID, "customerName", FacetType.STRING, Cardinality.SINGLE, V2);
        Facet regionV2 = facet(REGION_ID, "region", FacetType.STRING, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(
                List.of(customerNameV1, customerNameV2, regionV2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged).hasSize(2);
        Facet region = merged.stream()
                .filter(f -> f.path().asSlugPath().equals("region"))
                .findFirst()
                .orElseThrow();
        assertThat(region.definedInVersions()).containsExactly(V2);
    }

    @Test
    void pinnedSelectsOnlyThatVersionsDefinition() {
        Facet customerNameV1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet customerNameV2 = facet(CUSTOMER_NAME_ID, "customerName", FacetType.STRING, Cardinality.SINGLE, V2);
        Facet regionV2 = facet(REGION_ID, "region", FacetType.STRING, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(
                List.of(customerNameV1, customerNameV2, regionV2), new VersionPolicy.Pinned(V1), List.of(V1, V2));

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).path().asSlugPath()).isEqualTo("custName");
        assertThat(merged.get(0).definedInVersions()).containsExactly(V1);
    }

    @Test
    void pinnedOmitsFacetAbsentFromPinnedVersion() {
        Facet customerNameV1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet customerNameV2 = facet(CUSTOMER_NAME_ID, "customerName", FacetType.STRING, Cardinality.SINGLE, V2);
        Facet regionV2 = facet(REGION_ID, "region", FacetType.STRING, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(
                List.of(customerNameV1, customerNameV2, regionV2), new VersionPolicy.Pinned(V1), List.of(V1, V2));

        assertThat(merged).noneMatch(f -> f.path().asSlugPath().equals("region"));
    }

    @Test
    void pinnedUnknownVersionThrowsVersionNotFound() {
        Facet v1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);

        assertThatExceptionOfType(FacetResolutionException.class)
                .isThrownBy(() ->
                        FacetMerger.merge(List.of(v1), new VersionPolicy.Pinned(UNKNOWN_VERSION), List.of(V1, V2)))
                .satisfies(e -> assertThat(e.code()).isEqualTo(FacetErrorCode.VERSION_NOT_FOUND));
    }

    @Test
    void pinnedNeverProducesConflict() {
        Facet v1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet v2 = facet(ISSUED_AT_ID, "issuedAt", FacetType.DATETIME, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Pinned(V1), List.of(V1, V2));

        assertThat(merged.get(0).conflict()).isEmpty();
    }

    @Test
    void intersectionKeepsOnlyFacetsInEveryInputVersion() {
        Facet customerNameV1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet customerNameV2 = facet(CUSTOMER_NAME_ID, "customerName", FacetType.STRING, Cardinality.SINGLE, V2);
        Facet regionV2 = facet(REGION_ID, "region", FacetType.STRING, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(
                List.of(customerNameV1, customerNameV2, regionV2), new VersionPolicy.Intersection(), List.of(V1, V2));

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).path().asSlugPath()).isEqualTo("customerName");
    }

    @Test
    void intersectionOmitsVersionExclusiveFacet() {
        Facet customerNameV1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet customerNameV2 = facet(CUSTOMER_NAME_ID, "customerName", FacetType.STRING, Cardinality.SINGLE, V2);
        Facet regionV2 = facet(REGION_ID, "region", FacetType.STRING, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(
                List.of(customerNameV1, customerNameV2, regionV2), new VersionPolicy.Intersection(), List.of(V1, V2));

        assertThat(merged).noneMatch(f -> f.path().asSlugPath().equals("region"));
    }

    @Test
    void intersectionEmptyResultThrowsEmptyIntersection() {
        Facet onlyV1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet onlyV2 = facet(REGION_ID, "region", FacetType.STRING, Cardinality.SINGLE, V2);

        assertThatExceptionOfType(FacetResolutionException.class)
                .isThrownBy(() ->
                        FacetMerger.merge(List.of(onlyV1, onlyV2), new VersionPolicy.Intersection(), List.of(V1, V2)))
                .satisfies(e -> assertThat(e.code()).isEqualTo(FacetErrorCode.EMPTY_INTERSECTION));
    }

    @Test
    void intersectionUsesVersionsPresentInInputNotGlobalVersionOrder() {
        Facet customerNameV1 = facet(CUSTOMER_NAME_ID, "custName", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet customerNameV2 = facet(CUSTOMER_NAME_ID, "customerName", FacetType.STRING, Cardinality.SINGLE, V2);

        // versionOrder carries v3 too, but no input facet is defined in v3. Using versionOrder as
        // the "in scope" set would wrongly demand a v3 definition and raise EMPTY_INTERSECTION.
        List<Facet> merged = FacetMerger.merge(
                List.of(customerNameV1, customerNameV2), new VersionPolicy.Intersection(), List.of(V1, V2, V3));

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).path().asSlugPath()).isEqualTo("customerName");
    }

    @Test
    void conflictKeepsFacetAndAttachesFacetConflict() {
        Facet v1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet v2 = facet(ISSUED_AT_ID, "issuedAt", FacetType.DATETIME, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).conflict()).isPresent();
    }

    @Test
    void conflictDoesNotThrow() {
        Facet v1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet v2 = facet(ISSUED_AT_ID, "issuedAt", FacetType.DATETIME, Cardinality.SINGLE, V2);

        assertThatCode(() -> FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2)))
                .doesNotThrowAnyException();
    }

    @Test
    void conflictListsEveryDefinitionNotOnlyDiffering() {
        Facet v1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet v2 = facet(ISSUED_AT_ID, "issuedAt", FacetType.DATETIME, Cardinality.SINGLE, V2);
        Facet v3 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V3);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2, v3), new VersionPolicy.Union(), List.of(V1, V2, V3));

        assertThat(merged.get(0).conflict()).isPresent();
        assertThat(merged.get(0).conflict().orElseThrow().definitions()).hasSize(3);
        assertThat(merged.get(0).conflict().orElseThrow().definitions())
                .extracting(d -> d.versionIri())
                .containsExactly(V1, V2, V3);
    }

    @Test
    void conflictTypeMismatchResolvesToStringWhenNeitherIsUnknown() {
        Facet v1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet v2 = facet(ISSUED_AT_ID, "issuedAt", FacetType.DATETIME, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged.get(0).type()).isEqualTo(FacetType.STRING);
    }

    @Test
    void conflictTypeMismatchResolvesToUnknownWhenAnyDefinitionIsUnknown() {
        Facet v1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet v2 = facet(ISSUED_AT_ID, "issuedAt", FacetType.UNKNOWN, Cardinality.SINGLE, V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged.get(0).type()).isEqualTo(FacetType.UNKNOWN);
    }

    @Test
    void conflictCardinalityMismatchResolvesToMany() {
        Facet v1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet v2 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.MANY, V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged.get(0).cardinality()).isEqualTo(Cardinality.MANY);
    }

    @Test
    void conflictCapabilitiesAreAndedAcrossDefinitions() {
        Capabilities allCapable = new Capabilities(true, true, true, true);
        Capabilities notSortable = new Capabilities(true, true, true, false);
        Facet v1 = facet(
                ISSUED_AT_ID,
                "issuedAt",
                FacetType.STRING,
                Cardinality.SINGLE,
                allCapable,
                OperatorRestriction.unrestricted(),
                null,
                Optional.empty(),
                InferenceSupport.ASSERTED_ONLY,
                V1);
        Facet v2 = facet(
                ISSUED_AT_ID,
                "issuedAt",
                FacetType.STRING,
                Cardinality.SINGLE,
                notSortable,
                OperatorRestriction.unrestricted(),
                null,
                Optional.empty(),
                InferenceSupport.ASSERTED_ONLY,
                V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged.get(0).capabilities()).isEqualTo(new Capabilities(true, true, true, false));
    }

    @Test
    void conflictRestrictionIntersectsAllowedList() {
        OperatorRestriction v1Restriction = new OperatorRestriction(List.of("is", "is not", "contains"), List.of());
        OperatorRestriction v2Restriction = new OperatorRestriction(List.of("is", "contains"), List.of());
        Facet v1 = facet(
                ISSUED_AT_ID,
                "issuedAt",
                FacetType.STRING,
                Cardinality.SINGLE,
                ALL_TRUE,
                v1Restriction,
                null,
                Optional.empty(),
                InferenceSupport.ASSERTED_ONLY,
                V1);
        Facet v2 = facet(
                ISSUED_AT_ID,
                "issuedAt",
                FacetType.STRING,
                Cardinality.SINGLE,
                ALL_TRUE,
                v2Restriction,
                null,
                Optional.empty(),
                InferenceSupport.ASSERTED_ONLY,
                V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged.get(0).restriction().allowed()).containsExactly("is", "contains");
    }

    @Test
    void conflictRestrictionUnionsDeniedList() {
        OperatorRestriction v1Restriction = new OperatorRestriction(List.of(), List.of("between"));
        OperatorRestriction v2Restriction = new OperatorRestriction(List.of(), List.of("is like"));
        Facet v1 = facet(
                ISSUED_AT_ID,
                "issuedAt",
                FacetType.STRING,
                Cardinality.SINGLE,
                ALL_TRUE,
                v1Restriction,
                null,
                Optional.empty(),
                InferenceSupport.ASSERTED_ONLY,
                V1);
        Facet v2 = facet(
                ISSUED_AT_ID,
                "issuedAt",
                FacetType.STRING,
                Cardinality.SINGLE,
                ALL_TRUE,
                v2Restriction,
                null,
                Optional.empty(),
                InferenceSupport.ASSERTED_ONLY,
                V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged.get(0).restriction().denied()).containsExactly("between", "is like");
    }

    @Test
    void conflictRestrictionEmptyAllowedActsAsNoOpinion() {
        OperatorRestriction v1Restriction = OperatorRestriction.unrestricted();
        OperatorRestriction v2Restriction = new OperatorRestriction(List.of("is", "is not"), List.of());
        Facet v1 = facet(
                ISSUED_AT_ID,
                "issuedAt",
                FacetType.STRING,
                Cardinality.SINGLE,
                ALL_TRUE,
                v1Restriction,
                null,
                Optional.empty(),
                InferenceSupport.ASSERTED_ONLY,
                V1);
        Facet v2 = facet(
                ISSUED_AT_ID,
                "issuedAt",
                FacetType.STRING,
                Cardinality.SINGLE,
                ALL_TRUE,
                v2Restriction,
                null,
                Optional.empty(),
                InferenceSupport.ASSERTED_ONLY,
                V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged.get(0).restriction().allowed()).containsExactly("is", "is not");
    }

    @Test
    void conflictInferenceMismatchResolvesToMayBeInferred() {
        Facet v1 = facet(
                ISSUED_AT_ID,
                "issuedAt",
                FacetType.STRING,
                Cardinality.SINGLE,
                ALL_TRUE,
                OperatorRestriction.unrestricted(),
                null,
                Optional.empty(),
                InferenceSupport.ASSERTED_ONLY,
                V1);
        Facet v2 = facet(
                ISSUED_AT_ID,
                "issuedAt",
                FacetType.STRING,
                Cardinality.SINGLE,
                ALL_TRUE,
                OperatorRestriction.unrestricted(),
                null,
                Optional.empty(),
                InferenceSupport.MAY_BE_INFERRED,
                V2);

        List<Facet> merged = FacetMerger.merge(List.of(v1, v2), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged.get(0).inference()).isEqualTo(InferenceSupport.MAY_BE_INFERRED);
    }

    @Test
    void conflictReasonDescribesWhatChanged() {
        Facet typeOnlyV1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet typeOnlyV2 = facet(ISSUED_AT_ID, "issuedAt", FacetType.DATETIME, Cardinality.SINGLE, V2);
        Facet cardinalityOnlyV1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet cardinalityOnlyV2 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.MANY, V2);
        Facet bothV1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, V1);
        Facet bothV2 = facet(ISSUED_AT_ID, "issuedAt", FacetType.DATETIME, Cardinality.MANY, V2);

        String typeReason = FacetMerger.merge(
                        List.of(typeOnlyV1, typeOnlyV2), new VersionPolicy.Union(), List.of(V1, V2))
                .get(0)
                .conflict()
                .orElseThrow()
                .reason();
        String cardinalityReason = FacetMerger.merge(
                        List.of(cardinalityOnlyV1, cardinalityOnlyV2), new VersionPolicy.Union(), List.of(V1, V2))
                .get(0)
                .conflict()
                .orElseThrow()
                .reason();
        String bothReason = FacetMerger.merge(List.of(bothV1, bothV2), new VersionPolicy.Union(), List.of(V1, V2))
                .get(0)
                .conflict()
                .orElseThrow()
                .reason();

        assertThat(typeReason).isEqualTo("datatype changed between versions");
        assertThat(cardinalityReason).isEqualTo("cardinality changed between versions");
        assertThat(bothReason).isEqualTo("datatype and cardinality changed between versions");
    }

    @Test
    void mergeRejectsInputFacetWithMultipleDefinedInVersions() {
        Facet invalid =
                facetWithVersions(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, Set.of(V1, V2));

        assertThatThrownBy(() -> FacetMerger.merge(List.of(invalid), new VersionPolicy.Union(), List.of(V1, V2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuedAt");
    }

    @Test
    void mergeRejectsVersionNotInVersionOrder() {
        Facet v1 = facet(ISSUED_AT_ID, "issuedAt", FacetType.STRING, Cardinality.SINGLE, UNKNOWN_VERSION);

        assertThatThrownBy(() -> FacetMerger.merge(List.of(v1), new VersionPolicy.Union(), List.of(V1, V2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UNKNOWN_VERSION);
    }

    @Test
    void mergeOutputIsSortedBySlugPath() {
        Facet zeta = facet(
                UUID.fromString("00000000-0000-0000-0000-0000000000d1"),
                "zeta",
                FacetType.STRING,
                Cardinality.SINGLE,
                V1);
        Facet alpha = facet(
                UUID.fromString("00000000-0000-0000-0000-0000000000d2"),
                "alpha",
                FacetType.STRING,
                Cardinality.SINGLE,
                V1);
        Facet mid = facet(
                UUID.fromString("00000000-0000-0000-0000-0000000000d3"),
                "mid",
                FacetType.STRING,
                Cardinality.SINGLE,
                V1);

        List<Facet> merged = FacetMerger.merge(List.of(zeta, alpha, mid), new VersionPolicy.Union(), List.of(V1, V2));

        assertThat(merged).extracting(f -> f.path().asSlugPath()).containsExactly("alpha", "mid", "zeta");
    }
}
