package org.sequeless.ontology.facet.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.sequeless.ontology.facet.core.api.ClassRef;
import org.sequeless.ontology.facet.core.api.FacetScopeRequest;
import org.sequeless.ontology.facet.core.api.LocalizedText;
import org.sequeless.ontology.facet.core.api.Principal;
import org.sequeless.ontology.facet.core.api.StampMode;
import org.sequeless.ontology.facet.core.api.TraversalConfig;
import org.sequeless.ontology.facet.core.api.VersionPolicy;

class StampCalculatorTest {

    private static final UUID ROOT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ROOT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static ClassRef classRef(UUID id, String slug) {
        return new ClassRef(id, slug, "urn:class:" + slug, new LocalizedText(slug, "en"));
    }

    private static FacetScopeRequest request(
            String tenantId,
            UUID rootId,
            VersionPolicy versionPolicy,
            TraversalConfig traversal,
            Optional<Principal> principal,
            Optional<Locale> locale) {
        return new FacetScopeRequest(
                tenantId, classRef(rootId, "Invoice"), versionPolicy, traversal, principal, locale);
    }

    private static FacetScopeRequest fixedInputRequest() {
        return request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("u-42", Set.of("analyst", "admin"))),
                Optional.empty());
    }

    @Test
    void versionOnlySortsAndJoinsVersions() {
        FacetScopeRequest request = request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.VERSION_ONLY, false),
                Optional.empty(),
                Optional.empty());

        String stamp = StampCalculator.stamp(request, Set.of("urn:ont:v2", "urn:ont:v1"));

        assertThat(stamp).isEqualTo("urn:ont:v1,urn:ont:v2");
    }

    @Test
    void versionOnlyIsOrderIndependent() {
        FacetScopeRequest request = request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.VERSION_ONLY, false),
                Optional.empty(),
                Optional.empty());

        String forward = StampCalculator.stamp(request, Set.of("urn:ont:v1", "urn:ont:v2", "urn:ont:v3"));
        String backward = StampCalculator.stamp(request, Set.of("urn:ont:v3", "urn:ont:v2", "urn:ont:v1"));

        assertThat(forward).isEqualTo(backward);
    }

    @Test
    void versionOnlyProducesUnversionedSentinelForEmptyVersions() {
        FacetScopeRequest request = request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.VERSION_ONLY, false),
                Optional.empty(),
                Optional.empty());

        String stamp = StampCalculator.stamp(request, Set.of());

        assertThat(stamp).isEqualTo("unversioned");
    }

    @Test
    void contentHashMatchesKnownDigestForFixedInput() {
        String stamp = StampCalculator.stamp(fixedInputRequest(), Set.of("urn:ont:v1", "urn:ont:v2"));

        assertThat(stamp).isEqualTo("sha256:016c25e6789a267f93e3683948722cecd4b76f2d56d1aeb6f46889018fe687dc");
    }

    @Test
    void contentHashIsOrderIndependentForVersionsSet() {
        String forward = StampCalculator.stamp(fixedInputRequest(), Set.of("urn:ont:v1", "urn:ont:v2"));
        String backward = StampCalculator.stamp(fixedInputRequest(), Set.of("urn:ont:v2", "urn:ont:v1"));

        assertThat(forward).isEqualTo(backward);
    }

    @Test
    void contentHashIsOrderIndependentForPrincipalRoles() {
        FacetScopeRequest forwardRequest = request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("u-42", Set.of("analyst", "admin"))),
                Optional.empty());
        FacetScopeRequest backwardRequest = request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("u-42", Set.of("admin", "analyst"))),
                Optional.empty());

        String forward = StampCalculator.stamp(forwardRequest, Set.of("urn:ont:v1"));
        String backward = StampCalculator.stamp(backwardRequest, Set.of("urn:ont:v1"));

        assertThat(forward).isEqualTo(backward);
    }

    @Test
    void contentHashDiffersWhenTenantDiffers() {
        FacetScopeRequest acme = fixedInputRequest();
        FacetScopeRequest other = request(
                "globex",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("u-42", Set.of("analyst", "admin"))),
                Optional.empty());

        String versions = "urn:ont:v1";
        assertThat(StampCalculator.stamp(acme, Set.of(versions)))
                .isNotEqualTo(StampCalculator.stamp(other, Set.of(versions)));
    }

    @Test
    void contentHashDiffersWhenRootClassDiffers() {
        FacetScopeRequest first = fixedInputRequest();
        FacetScopeRequest second = request(
                "acme",
                OTHER_ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("u-42", Set.of("analyst", "admin"))),
                Optional.empty());

        String versions = "urn:ont:v1";
        assertThat(StampCalculator.stamp(first, Set.of(versions)))
                .isNotEqualTo(StampCalculator.stamp(second, Set.of(versions)));
    }

    @Test
    void contentHashDiffersWhenVersionPolicyDiffers() {
        FacetScopeRequest union = fixedInputRequest();
        FacetScopeRequest pinned = request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Pinned("urn:ont:v1"),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("u-42", Set.of("analyst", "admin"))),
                Optional.empty());

        String versions = "urn:ont:v1";
        assertThat(StampCalculator.stamp(union, Set.of(versions)))
                .isNotEqualTo(StampCalculator.stamp(pinned, Set.of(versions)));
    }

    @Test
    void contentHashDiffersWhenMaxDepthDiffers() {
        FacetScopeRequest depthThree = fixedInputRequest();
        FacetScopeRequest depthFive = request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(5), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("u-42", Set.of("analyst", "admin"))),
                Optional.empty());

        String versions = "urn:ont:v1";
        assertThat(StampCalculator.stamp(depthThree, Set.of(versions)))
                .isNotEqualTo(StampCalculator.stamp(depthFive, Set.of(versions)));
    }

    @Test
    void contentHashDiffersWhenPrincipalAbsentVsPresent() {
        FacetScopeRequest withPrincipal = fixedInputRequest();
        FacetScopeRequest withoutPrincipal = request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.empty(),
                Optional.empty());

        String versions = "urn:ont:v1";
        assertThat(StampCalculator.stamp(withPrincipal, Set.of(versions)))
                .isNotEqualTo(StampCalculator.stamp(withoutPrincipal, Set.of(versions)));
    }

    @Test
    void contentHashIgnoresLocale() {
        FacetScopeRequest english = request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("u-42", Set.of("analyst", "admin"))),
                Optional.of(Locale.ENGLISH));
        FacetScopeRequest french = request(
                "acme",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("u-42", Set.of("analyst", "admin"))),
                Optional.of(Locale.FRENCH));

        String versions = "urn:ont:v1";
        assertThat(StampCalculator.stamp(english, Set.of(versions)))
                .isEqualTo(StampCalculator.stamp(french, Set.of(versions)));
    }

    @Test
    void contentHashIsStableAcrossRepeatedCalls() {
        FacetScopeRequest request = fixedInputRequest();
        Set<String> versions = Set.of("urn:ont:v1", "urn:ont:v2");

        String first = StampCalculator.stamp(request, versions);
        String second = StampCalculator.stamp(request, versions);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void contentHashIsInjectiveAcrossFieldBoundaries() {
        // Without a length prefix on every field, tenant "a" followed by role "bc" would render the
        // same characters as tenant "ab" followed by role "c" once concatenated naively. The
        // netstring framework must keep these apart.
        FacetScopeRequest tenantA = request(
                "a",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("bc", Set.of())),
                Optional.empty());
        FacetScopeRequest tenantAb = request(
                "ab",
                ROOT_ID,
                new VersionPolicy.Union(),
                new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false),
                Optional.of(new Principal("c", Set.of())),
                Optional.empty());

        assertThat(StampCalculator.stamp(tenantA, Set.of())).isNotEqualTo(StampCalculator.stamp(tenantAb, Set.of()));
    }
}
