package org.sequeless.ontology.facet.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FacetScopeTest {

    private static final String V1 = "urn:ont:v1";

    private static LocalizedText label(String value) {
        return new LocalizedText(value, "en");
    }

    private static ClassRef classRef(String slug) {
        return new ClassRef(UUID.nameUUIDFromBytes(slug.getBytes()), slug, "urn:class:" + slug, label(slug));
    }

    private static FacetScopeRequest request(String tenantId) {
        return new FacetScopeRequest(
                tenantId,
                classRef("Invoice"),
                new VersionPolicy.Union(),
                TraversalConfig.defaults(),
                Optional.empty(),
                Optional.empty());
    }

    private static Facet facet(String slug) {
        return new Facet(
                new FacetPath(List.of(new PathSegment(UUID.nameUUIDFromBytes(slug.getBytes()), slug, null))),
                FacetType.STRING,
                null,
                Cardinality.SINGLE,
                new Capabilities(true, true, false, true),
                label(slug),
                null,
                Optional.empty(),
                OperatorRestriction.unrestricted(),
                InferenceSupport.ASSERTED_ONLY,
                Set.of(V1),
                Optional.empty());
    }

    @Test
    void traversalConfigDefaultsMatchSpec() {
        assertThat(TraversalConfig.defaults())
                .isEqualTo(new TraversalConfig(OptionalInt.of(3), true, StampMode.VERSION_ONLY, false));
    }

    @Test
    void traversalConfigRejectsMaxDepthLessThanOne() {
        assertThatThrownBy(() -> new TraversalConfig(OptionalInt.of(0), true, StampMode.VERSION_ONLY, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepth");
    }

    @Test
    void traversalConfigAllowsAbsentMaxDepth() {
        TraversalConfig unbounded = new TraversalConfig(OptionalInt.empty(), true, StampMode.VERSION_ONLY, false);

        assertThat(unbounded.maxDepth()).isEmpty();
    }

    @Test
    void pinnedRejectsBlankVersionIri() {
        assertThatThrownBy(() -> new VersionPolicy.Pinned("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versionIri");
    }

    @Test
    void distinctUnionInstancesAreEqual() {
        // Record equality derives purely from the component list, and Union has none — so two
        // separate instances are always equal. This is why no singleton constant is needed.
        assertThat(new VersionPolicy.Union()).isEqualTo(new VersionPolicy.Union());
        assertThat(new VersionPolicy.Union()).hasSameHashCodeAs(new VersionPolicy.Union());
    }

    @Test
    void distinctIntersectionInstancesAreEqual() {
        assertThat(new VersionPolicy.Intersection()).isEqualTo(new VersionPolicy.Intersection());
        assertThat(new VersionPolicy.Intersection()).hasSameHashCodeAs(new VersionPolicy.Intersection());
    }

    @Test
    void pinnedInstancesWithDifferentVersionsAreNotEqual() {
        assertThat(new VersionPolicy.Pinned(V1)).isNotEqualTo(new VersionPolicy.Pinned("urn:ont:v2"));
    }

    @Test
    void facetScopeRequestRejectsBlankTenantId() {
        assertThatThrownBy(() -> request("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void principalDefensiveCopyIsolatesFromSourceMutation() {
        Set<String> roles = new HashSet<>(Set.of("analyst"));
        Principal principal = new Principal("u-1", roles);

        roles.add("admin");

        assertThat(principal.roles()).containsExactly("analyst");
    }

    @Test
    void facetScopeDefensiveCopyIsolatesFromSourceMutation() {
        List<Facet> facets = new ArrayList<>(List.of(facet("status")));
        FacetScope scope =
                new FacetScope(classRef("Invoice"), facets, new ArrayList<>(), V1, Set.of(V1), request("acme"));

        facets.add(facet("total"));

        assertThat(scope.facets()).hasSize(1);
    }

    @Test
    void facetScopeRejectsBlankStamp() {
        assertThatThrownBy(() ->
                        new FacetScope(classRef("Invoice"), List.of(), List.of(), "  ", Set.of(V1), request("acme")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stamp");
    }
}
