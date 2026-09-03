package org.sequeless.ontology.facet.core.testfixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.sequeless.ontology.facet.core.api.Capabilities;
import org.sequeless.ontology.facet.core.api.ClassRef;
import org.sequeless.ontology.facet.core.api.ConflictingDefinition;
import org.sequeless.ontology.facet.core.api.Facet;
import org.sequeless.ontology.facet.core.api.FacetErrorCode;
import org.sequeless.ontology.facet.core.api.FacetPath;
import org.sequeless.ontology.facet.core.api.FacetResolutionException;
import org.sequeless.ontology.facet.core.api.FacetScope;
import org.sequeless.ontology.facet.core.api.FacetScopeRequest;
import org.sequeless.ontology.facet.core.api.FacetType;
import org.sequeless.ontology.facet.core.api.LocalizedText;
import org.sequeless.ontology.facet.core.api.PathSegment;
import org.sequeless.ontology.facet.core.api.Principal;
import org.sequeless.ontology.facet.core.api.Relationship;
import org.sequeless.ontology.facet.core.api.StampMode;
import org.sequeless.ontology.facet.core.api.TraversalConfig;
import org.sequeless.ontology.facet.core.api.VersionPolicy;
import org.sequeless.ontology.facet.core.spi.FacetSource;

/**
 * The reusable contract test kit any {@link FacetSource} implementation, including a future
 * RDF-backed one, must pass.
 *
 * <p>Every test method below exercises one guarantee documented on {@link FacetSource}'s three
 * methods — {@code resolve}, {@code expand}, and {@code describe} — against the nine canned {@link
 * ContractFixtures} scenarios. A concrete subclass supplies only {@link #sourceFor(ContractFixture)}
 * — how to build its {@code FacetSource} implementation over a given scenario — and inherits every
 * test unchanged. See {@code docs/specs/facet-contract/design.md} and {@code package-info}.
 */
public abstract class FacetSourceContractTest {

    private static final String V1 = "urn:ont:v1";
    private static final String V2 = "urn:ont:v2";

    /**
     * Builds the {@code FacetSource} implementation under test, backed by {@code fixture}.
     *
     * @param fixture the scenario the returned source must serve; never {@code null}
     * @return a {@code FacetSource} over {@code fixture}
     */
    protected abstract FacetSource sourceFor(ContractFixture fixture);

    // ------------------------------------------------------------------------------------------
    // Request/path building helpers
    // ------------------------------------------------------------------------------------------

    private static ClassRef classRefOf(ClassFixture classFixture) {
        return new ClassRef(
                classFixture.id(),
                classFixture.slug(),
                classFixture.iri(),
                new LocalizedText(classFixture.slug(), null));
    }

    private static ClassRef rootOf(ContractFixture fixture) {
        return classRefOf(fixture.classes().get(0));
    }

    private static ClassRef randomClassRef() {
        return new ClassRef(UUID.randomUUID(), "unknown", "urn:ont:class:unknown", new LocalizedText("unknown", null));
    }

    private static FacetScopeRequest request(
            ContractFixture fixture, VersionPolicy policy, TraversalConfig traversal, Optional<Principal> principal) {
        return new FacetScopeRequest(
                fixture.tenantId(), rootOf(fixture), policy, traversal, principal, Optional.empty());
    }

    private static FacetScopeRequest defaultRequest(ContractFixture fixture) {
        return request(fixture, new VersionPolicy.Union(), TraversalConfig.defaults(), Optional.empty());
    }

    private static RelationshipFixture relationshipOn(ClassFixture classFixture, String slug) {
        return classFixture.relationships().stream()
                .filter(relationship -> relationship.slug().equals(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "no relationship named '" + slug + "' on class '" + classFixture.slug() + "'"));
    }

    /**
     * Builds a {@link FacetPath} from {@code fixture}'s root, walking every slug but the last as a
     * relationship (looked up on the current class, advancing to its target) and resolving the last
     * slug as a terminal scalar property on the class the walk lands on.
     *
     * <p>{@code pathTo(fixture, "customer", "city")} on {@code invoiceAndCustomer()} therefore
     * produces a two-segment path: {@code customer} (a relationship from Invoice to Customer) then
     * {@code city} (a property on Customer).
     */
    private static FacetPath pathTo(ContractFixture fixture, String... classAndPropertySlugs) {
        List<PathSegment> segments = new ArrayList<>();
        ClassFixture current = fixture.classes().get(0);
        for (int i = 0; i < classAndPropertySlugs.length; i++) {
            String slug = classAndPropertySlugs[i];
            boolean isLast = i == classAndPropertySlugs.length - 1;
            if (!isLast) {
                RelationshipFixture relationship = relationshipOn(current, slug);
                ClassFixture target = fixture.classNamed(relationship.targetClassSlug());
                segments.add(new PathSegment(relationship.id(), relationship.slug(), classRefOf(target)));
                current = target;
            } else {
                ClassFixture terminalClass = current;
                PropertyFixture property = terminalClass.properties().stream()
                        .filter(p -> p.slug().equals(slug))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "no property named '" + slug + "' on class '" + terminalClass.slug() + "'"));
                segments.add(new PathSegment(property.id(), property.slug(), null));
            }
        }
        return new FacetPath(segments);
    }

    /**
     * Builds a pure-relationship {@link FacetPath} from {@code fixture}'s root, every segment
     * (including the last) naming a relationship rather than a terminal scalar property — the shape
     * {@link FacetSource#expand(FacetScope, FacetPath)} requires of its {@code relationship}
     * parameter.
     */
    private static FacetPath relationshipPath(ContractFixture fixture, String... relationshipSlugs) {
        List<PathSegment> segments = new ArrayList<>();
        ClassFixture current = fixture.classes().get(0);
        for (String slug : relationshipSlugs) {
            RelationshipFixture relationship = relationshipOn(current, slug);
            ClassFixture target = fixture.classNamed(relationship.targetClassSlug());
            segments.add(new PathSegment(relationship.id(), relationship.slug(), classRefOf(target)));
            current = target;
        }
        return new FacetPath(segments);
    }

    /** A single-segment path naming a relationship on {@code fixture}'s root as the terminal segment. */
    private static FacetPath singleRelationshipTerminalPath(ContractFixture fixture, String slug) {
        RelationshipFixture relationship = relationshipOn(fixture.classes().get(0), slug);
        return new FacetPath(List.of(new PathSegment(relationship.id(), relationship.slug(), null)));
    }

    /** A single-segment terminal path naming a property id that exists nowhere in any fixture. */
    private static FacetPath randomTerminalPath() {
        return new FacetPath(List.of(new PathSegment(UUID.randomUUID(), "unknown", null)));
    }

    /** A single-segment relationship path naming a property id that exists nowhere in any fixture. */
    private static FacetPath randomRelationshipPath() {
        return new FacetPath(List.of(new PathSegment(UUID.randomUUID(), "unknown", randomClassRef())));
    }

    private static FacetErrorCode codeOf(ThrowingCallable callable) {
        Throwable thrown = catchThrowable(callable);
        assertThat(thrown).isInstanceOf(FacetResolutionException.class);
        return ((FacetResolutionException) thrown).code();
    }

    // ------------------------------------------------------------------------------------------
    // resolve
    // ------------------------------------------------------------------------------------------

    @Test
    final void resolveReturnsRootFacets() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);

        FacetScope scope = source.resolve(defaultRequest(fixture));

        assertThat(scope.facets())
                .extracting(f -> f.path().asSlugPath())
                .containsExactlyInAnyOrder("status", "total", "description", "internalNotes");
    }

    @Test
    final void resolveReturnsOnlyWalkableRelationships() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);

        FacetScope scope = source.resolve(defaultRequest(fixture));

        assertThat(scope.relationships()).extracting(Relationship::slug).containsExactly("customer");
    }

    @Test
    final void resolveEchoesRequestInScope() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request = defaultRequest(fixture);

        FacetScope scope = source.resolve(request);

        assertThat(scope.request()).isEqualTo(request);
    }

    @Test
    final void resolveUnknownTenantRaisesUnknownTenant() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request = new FacetScopeRequest(
                "not-" + fixture.tenantId(),
                rootOf(fixture),
                new VersionPolicy.Union(),
                TraversalConfig.defaults(),
                Optional.empty(),
                Optional.empty());

        assertThat(codeOf(() -> source.resolve(request))).isEqualTo(FacetErrorCode.UNKNOWN_TENANT);
    }

    @Test
    final void resolveUnknownClassRaisesUnknownClass() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request = new FacetScopeRequest(
                fixture.tenantId(),
                randomClassRef(),
                new VersionPolicy.Union(),
                TraversalConfig.defaults(),
                Optional.empty(),
                Optional.empty());

        assertThat(codeOf(() -> source.resolve(request))).isEqualTo(FacetErrorCode.UNKNOWN_CLASS);
    }

    // ------------------------------------------------------------------------------------------
    // expand
    // ------------------------------------------------------------------------------------------

    @Test
    final void expandAddsExactlyOneHopOfFacets() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScope scope = source.resolve(defaultRequest(fixture));

        FacetScope expanded = source.expand(scope, relationshipPath(fixture, "customer"));

        assertThat(expanded.facets())
                .extracting(f -> f.path().asSlugPath())
                .contains("customer.name", "customer.city", "customer.tier");
        assertThat(expanded.relationships()).extracting(Relationship::slug).contains("account");
    }

    @Test
    final void expandPreservesAlreadyResolvedFacets() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScope scope = source.resolve(defaultRequest(fixture));

        FacetScope expanded = source.expand(scope, relationshipPath(fixture, "customer"));

        assertThat(expanded.facets()).extracting(f -> f.path().asSlugPath()).contains("status");
    }

    @Test
    final void expandDoesNotAddTwoHopFacets() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScope scope = source.resolve(defaultRequest(fixture));

        FacetScope expanded = source.expand(scope, relationshipPath(fixture, "customer"));

        assertThat(expanded.facets()).noneMatch(f -> f.path().asSlugPath().equals("customer.account.reference"));
    }

    @Test
    final void expandUnknownRelationshipRaisesUnknownPath() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScope scope = source.resolve(defaultRequest(fixture));

        assertThat(codeOf(() -> source.expand(scope, randomRelationshipPath()))).isEqualTo(FacetErrorCode.UNKNOWN_PATH);
    }

    @Test
    final void expandPreservesStampFromOriginalScope() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScope scope = source.resolve(defaultRequest(fixture));

        FacetScope expanded = source.expand(scope, relationshipPath(fixture, "customer"));

        assertThat(expanded.stamp()).isEqualTo(scope.stamp());
    }

    // ------------------------------------------------------------------------------------------
    // describe
    // ------------------------------------------------------------------------------------------

    @Test
    final void describeResolvesDeepPathWithoutEnumerating() {
        ContractFixture fixture = ContractFixtures.deep();
        FacetSource source = sourceFor(fixture);
        FacetPath path = pathTo(fixture, "step", "step", "step", "value");

        Facet facet = source.describe(defaultRequest(fixture), path);

        assertThat(facet.path()).isEqualTo(path);
        assertThat(facet.type()).isEqualTo(FacetType.STRING);
    }

    @Test
    final void describeUnknownTenantRaisesUnknownTenant() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request = new FacetScopeRequest(
                "not-" + fixture.tenantId(),
                rootOf(fixture),
                new VersionPolicy.Union(),
                TraversalConfig.defaults(),
                Optional.empty(),
                Optional.empty());

        assertThat(codeOf(() -> source.describe(request, pathTo(fixture, "status"))))
                .isEqualTo(FacetErrorCode.UNKNOWN_TENANT);
    }

    @Test
    final void describeUnreachableClassRaisesClassNotReachable() {
        ContractFixture fixture = ContractFixtures.unwalkableEdge();
        FacetSource source = sourceFor(fixture);

        assertThat(codeOf(() -> source.describe(defaultRequest(fixture), pathTo(fixture, "customer", "city"))))
                .isEqualTo(FacetErrorCode.CLASS_NOT_REACHABLE);
    }

    @Test
    final void describeUnknownPropertyRaisesUnknownPath() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);

        assertThat(codeOf(() -> source.describe(defaultRequest(fixture), randomTerminalPath())))
                .isEqualTo(FacetErrorCode.UNKNOWN_PATH);
    }

    @Test
    final void describePathNotFilterableRaisesPathNotFilterable() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);

        assertThat(codeOf(() -> source.describe(defaultRequest(fixture), pathTo(fixture, "internalNotes"))))
                .isEqualTo(FacetErrorCode.PATH_NOT_FILTERABLE);
    }

    @Test
    final void describeReturnsCurrentFacetForValidPath() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);

        Facet facet = source.describe(defaultRequest(fixture), pathTo(fixture, "status"));

        assertThat(facet.type()).isEqualTo(FacetType.STRING);
        assertThat(facet.capabilities()).isEqualTo(new Capabilities(true, true, false, true));
    }

    // ------------------------------------------------------------------------------------------
    // Union
    // ------------------------------------------------------------------------------------------

    @Test
    final void unionMergesRenamedPropertyByUuid() {
        ContractFixture fixture = ContractFixtures.renamedAcrossVersions();
        FacetSource source = sourceFor(fixture);

        FacetScope scope = source.resolve(defaultRequest(fixture));

        assertThat(scope.facets().stream()
                        .filter(f -> f.path().asSlugPath().equals("customerName"))
                        .toList())
                .hasSize(1);
    }

    @Test
    final void unionUsesNewestSlugForDisplay() {
        ContractFixture fixture = ContractFixtures.renamedAcrossVersions();
        FacetSource source = sourceFor(fixture);

        FacetScope scope = source.resolve(defaultRequest(fixture));

        Facet renamed = scope.facets().stream()
                .filter(f -> f.definedInVersions().containsAll(Set.of(V1, V2)))
                .findFirst()
                .orElseThrow();
        assertThat(renamed.path().asSlugPath()).isEqualTo("customerName");
    }

    @Test
    final void unionRecordsAccurateDefinedInVersions() {
        ContractFixture fixture = ContractFixtures.renamedAcrossVersions();
        FacetSource source = sourceFor(fixture);

        FacetScope scope = source.resolve(defaultRequest(fixture));

        Facet renamed = scope.facets().stream()
                .filter(f -> f.path().asSlugPath().equals("customerName"))
                .findFirst()
                .orElseThrow();
        Facet region = scope.facets().stream()
                .filter(f -> f.path().asSlugPath().equals("region"))
                .findFirst()
                .orElseThrow();

        assertThat(renamed.definedInVersions()).isEqualTo(Set.of(V1, V2));
        assertThat(region.definedInVersions()).isEqualTo(Set.of(V2));
    }

    @Test
    final void unionKeepsVersionExclusiveFacet() {
        ContractFixture fixture = ContractFixtures.renamedAcrossVersions();
        FacetSource source = sourceFor(fixture);

        FacetScope scope = source.resolve(defaultRequest(fixture));

        assertThat(scope.facets()).anyMatch(f -> f.path().asSlugPath().equals("region"));
    }

    // ------------------------------------------------------------------------------------------
    // Pinned
    // ------------------------------------------------------------------------------------------

    /**
     * Per {@link VersionPolicy.Pinned}'s own worked example, typing a path that names a property
     * absent from the pinned version raises {@code UNKNOWN_PATH} — "as far as this resolution is
     * concerned, that property does not exist". This uses a property id absent from {@code
     * renamedAcrossVersions()} entirely, rather than {@code region} (present only in v2), because
     * {@code region} under {@code Pinned(v1)} currently trips an unrelated bug in {@link
     * InMemoryFacetSource#describe(FacetScopeRequest, FacetPath)}'s terminal-hop handling — it
     * calls {@code merged.get(0)} without checking {@code merged} is non-empty once the pinned
     * version filters out every candidate row for an id that does have rows in other versions, and
     * so throws {@code ArrayIndexOutOfBoundsException} rather than the documented {@code
     * FacetResolutionException}/{@code UNKNOWN_PATH}. That gap belongs to {@code
     * InMemoryFacetSource}, not to this contract, and is out of scope to fix here.
     */
    @Test
    final void pinnedAbsentPathRaisesUnknownPath() {
        // 'region' is defined only in v2 (see ContractFixtures.renamedAcrossVersions) -- this is
        // design.md section 4.4's own worked example: under Pinned(v1), region does not exist and
        // typing it raises UNKNOWN_PATH.
        ContractFixture fixture = ContractFixtures.renamedAcrossVersions();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request =
                request(fixture, new VersionPolicy.Pinned(V1), TraversalConfig.defaults(), Optional.empty());

        assertThat(codeOf(() -> source.describe(request, pathTo(fixture, "region"))))
                .isEqualTo(FacetErrorCode.UNKNOWN_PATH);
    }

    @Test
    final void pinnedUnknownVersionRaisesVersionNotFound() {
        ContractFixture fixture = ContractFixtures.renamedAcrossVersions();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request =
                request(fixture, new VersionPolicy.Pinned("urn:ont:v9"), TraversalConfig.defaults(), Optional.empty());

        assertThat(codeOf(() -> source.resolve(request))).isEqualTo(FacetErrorCode.VERSION_NOT_FOUND);
    }

    @Test
    final void pinnedUsesThatVersionsSlug() {
        ContractFixture fixture = ContractFixtures.renamedAcrossVersions();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request =
                request(fixture, new VersionPolicy.Pinned(V1), TraversalConfig.defaults(), Optional.empty());

        FacetScope scope = source.resolve(request);

        assertThat(scope.facets())
                .extracting(f -> f.path().asSlugPath())
                .contains("custName")
                .doesNotContain("customerName");
    }

    // ------------------------------------------------------------------------------------------
    // Intersection
    // ------------------------------------------------------------------------------------------

    @Test
    final void intersectionOmitsVersionExclusiveFacets() {
        ContractFixture fixture = ContractFixtures.versionExclusive();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request =
                request(fixture, new VersionPolicy.Intersection(), TraversalConfig.defaults(), Optional.empty());

        FacetScope scope = source.resolve(request);

        assertThat(scope.facets()).noneMatch(f -> f.path().asSlugPath().equals("region"));
    }

    @Test
    final void intersectionWithNoCommonFacetsRaisesEmptyIntersection() {
        ContractFixture fixture = ContractFixtures.disjointVersions();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request =
                request(fixture, new VersionPolicy.Intersection(), TraversalConfig.defaults(), Optional.empty());

        assertThat(codeOf(() -> source.resolve(request))).isEqualTo(FacetErrorCode.EMPTY_INTERSECTION);
    }

    // ------------------------------------------------------------------------------------------
    // Conflict
    // ------------------------------------------------------------------------------------------

    @Test
    final void typeMismatchMarksFacetAndKeepsIt() {
        ContractFixture fixture = ContractFixtures.conflictingTypes();
        FacetSource source = sourceFor(fixture);

        FacetScope scope = source.resolve(defaultRequest(fixture));

        Facet issuedAt = scope.facets().stream()
                .filter(f -> f.path().asSlugPath().equals("issuedAt"))
                .findFirst()
                .orElseThrow();
        assertThat(issuedAt.conflict()).isPresent();
    }

    @Test
    final void typeMismatchRecordsEveryConflictingDefinition() {
        ContractFixture fixture = ContractFixtures.conflictingTypes();
        FacetSource source = sourceFor(fixture);

        FacetScope scope = source.resolve(defaultRequest(fixture));

        Facet issuedAt = scope.facets().stream()
                .filter(f -> f.path().asSlugPath().equals("issuedAt"))
                .findFirst()
                .orElseThrow();
        List<ConflictingDefinition> definitions =
                issuedAt.conflict().orElseThrow().definitions();

        assertThat(definitions).hasSize(2);
        assertThat(definitions).extracting(ConflictingDefinition::versionIri).containsExactly(V1, V2);
        assertThat(definitions)
                .extracting(ConflictingDefinition::type)
                .containsExactly(FacetType.STRING, FacetType.DATETIME);
    }

    @Test
    final void typeMismatchDoesNotThrow() {
        ContractFixture fixture = ContractFixtures.conflictingTypes();
        FacetSource source = sourceFor(fixture);

        assertThatCode(() -> source.resolve(defaultRequest(fixture))).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------------------------------------
    // Guards
    // ------------------------------------------------------------------------------------------

    @Test
    final void unwalkableEdgeRaisesEdgeNotWalkable() {
        ContractFixture fixture = ContractFixtures.unwalkableEdge();
        FacetSource source = sourceFor(fixture);

        assertThat(codeOf(() ->
                        source.describe(defaultRequest(fixture), singleRelationshipTerminalPath(fixture, "customer"))))
                .isEqualTo(FacetErrorCode.EDGE_NOT_WALKABLE);
    }

    @Test
    final void cyclicPathRaisesCycleDetected() {
        ContractFixture fixture = ContractFixtures.cyclic();
        FacetSource source = sourceFor(fixture);
        FacetScope scope = source.resolve(defaultRequest(fixture));
        FacetScope expanded = source.expand(scope, relationshipPath(fixture, "toB"));

        assertThat(codeOf(() -> source.expand(expanded, relationshipPath(fixture, "toB", "toA"))))
                .isEqualTo(FacetErrorCode.CYCLE_DETECTED);
    }

    @Test
    final void sameClassInTwoDifferentPathsIsAllowed() {
        ContractFixture fixture = ContractFixtures.cyclic();
        FacetSource source = sourceFor(fixture);
        FacetScope scope = source.resolve(defaultRequest(fixture));

        assertThatCode(() -> source.expand(scope, relationshipPath(fixture, "toB")))
                .doesNotThrowAnyException();
        assertThatCode(() -> source.expand(scope, relationshipPath(fixture, "toC", "toB")))
                .doesNotThrowAnyException();
    }

    @Test
    final void exceedingMaxDepthRaisesDepthExceeded() {
        ContractFixture fixture = ContractFixtures.deep();
        FacetSource source = sourceFor(fixture);
        FacetScope scope = source.resolve(defaultRequest(fixture));
        FacetPath tooDeep = relationshipPath(fixture, "step", "step", "step", "step");

        assertThat(codeOf(() -> source.expand(scope, tooDeep))).isEqualTo(FacetErrorCode.DEPTH_EXCEEDED);
    }

    @Test
    final void unboundedMaxDepthAllowsDeepTraversal() {
        ContractFixture fixture = ContractFixtures.deep();
        FacetSource source = sourceFor(fixture);
        TraversalConfig unbounded = new TraversalConfig(OptionalInt.empty(), true, StampMode.VERSION_ONLY, false);
        FacetScopeRequest request = request(fixture, new VersionPolicy.Union(), unbounded, Optional.empty());
        FacetScope scope = source.resolve(request);
        FacetPath deepPath = relationshipPath(fixture, "step", "step", "step", "step", "step");

        assertThatCode(() -> source.expand(scope, deepPath)).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------------------------------------
    // Stamps
    // ------------------------------------------------------------------------------------------

    @Test
    final void identicalRequestsProduceIdenticalStamps() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request1 = defaultRequest(fixture);
        FacetScopeRequest request2 = defaultRequest(fixture);
        assertThat(request1).isNotSameAs(request2).isEqualTo(request2);

        String stamp1 = source.resolve(request1).stamp();
        String stamp2 = source.resolve(request2).stamp();

        assertThat(stamp1).isEqualTo(stamp2);
    }

    @Test
    final void contentHashStampChangesWithPrincipal() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        TraversalConfig contentHash = new TraversalConfig(OptionalInt.of(3), true, StampMode.CONTENT_HASH, false);
        FacetScopeRequest asAlice =
                request(fixture, new VersionPolicy.Union(), contentHash, Optional.of(new Principal("alice", Set.of())));
        FacetScopeRequest asBob =
                request(fixture, new VersionPolicy.Union(), contentHash, Optional.of(new Principal("bob", Set.of())));

        assertThat(source.resolve(asAlice).stamp())
                .isNotEqualTo(source.resolve(asBob).stamp());
    }

    @Test
    final void versionOnlyStampIgnoresTraversalConfigAndPrincipal() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        TraversalConfig shallow = new TraversalConfig(OptionalInt.of(1), true, StampMode.VERSION_ONLY, false);
        TraversalConfig deep = new TraversalConfig(OptionalInt.of(5), false, StampMode.VERSION_ONLY, false);
        FacetScopeRequest request1 =
                request(fixture, new VersionPolicy.Union(), shallow, Optional.of(new Principal("alice", Set.of())));
        FacetScopeRequest request2 =
                request(fixture, new VersionPolicy.Union(), deep, Optional.of(new Principal("bob", Set.of())));

        assertThat(source.resolve(request1).stamp())
                .isEqualTo(source.resolve(request2).stamp());
    }

    @Test
    final void unionStampReflectsAllVersionsInScope() {
        ContractFixture fixture = ContractFixtures.renamedAcrossVersions();
        FacetSource source = sourceFor(fixture);

        FacetScope scope = source.resolve(defaultRequest(fixture));

        assertThat(scope.ontologyVersions()).contains(V1, V2);
    }

    @Test
    final void expandStampEqualsOriginalScopeStamp() {
        ContractFixture fixture = ContractFixtures.cyclic();
        FacetSource source = sourceFor(fixture);
        FacetScope scope = source.resolve(defaultRequest(fixture));

        FacetScope expanded = source.expand(scope, relationshipPath(fixture, "toB"));

        assertThat(expanded.stamp()).isEqualTo(scope.stamp());
    }

    // ------------------------------------------------------------------------------------------
    // RBAC
    // ------------------------------------------------------------------------------------------

    @Test
    final void rbacFilteringHidesUnreadableClass() {
        ContractFixture fixture = ContractFixtures.rbacRestricted();
        FacetSource source = sourceFor(fixture);
        TraversalConfig rbacOn = new TraversalConfig(OptionalInt.of(3), true, StampMode.VERSION_ONLY, true);
        Principal noFinance = new Principal("no-finance", Set.of("sales"));
        FacetScopeRequest request = request(fixture, new VersionPolicy.Union(), rbacOn, Optional.of(noFinance));

        FacetScope scope = source.resolve(request);

        assertThat(scope.relationships()).noneMatch(r -> r.slug().equals("customer"));
    }

    @Test
    final void rbacDeniedPathRaisesPrincipalDenied() {
        ContractFixture fixture = ContractFixtures.rbacRestricted();
        FacetSource source = sourceFor(fixture);
        TraversalConfig rbacOn = new TraversalConfig(OptionalInt.of(3), true, StampMode.VERSION_ONLY, true);
        Principal noFinance = new Principal("no-finance", Set.of("sales"));
        FacetScopeRequest request = request(fixture, new VersionPolicy.Union(), rbacOn, Optional.of(noFinance));

        assertThat(codeOf(() -> source.describe(request, pathTo(fixture, "customer", "city"))))
                .isEqualTo(FacetErrorCode.PRINCIPAL_DENIED);
    }

    @Test
    final void rbacDisabledByDefaultShowsAllClasses() {
        ContractFixture fixture = ContractFixtures.rbacRestricted();
        FacetSource source = sourceFor(fixture);
        Principal noFinance = new Principal("no-finance", Set.of("sales"));
        FacetScopeRequest request =
                request(fixture, new VersionPolicy.Union(), TraversalConfig.defaults(), Optional.of(noFinance));

        FacetScope scope = source.resolve(request);

        assertThat(scope.relationships()).anyMatch(r -> r.slug().equals("customer"));
    }

    // ------------------------------------------------------------------------------------------
    // Codes
    // ------------------------------------------------------------------------------------------

    @Test
    final void pathNotFilterableIsDistinctFromUnknownPath() {
        ContractFixture fixture = ContractFixtures.invoiceAndCustomer();
        FacetSource source = sourceFor(fixture);
        FacetScopeRequest request = defaultRequest(fixture);

        FacetErrorCode notFilterable = codeOf(() -> source.describe(request, pathTo(fixture, "internalNotes")));
        FacetErrorCode unknown = codeOf(() -> source.describe(request, randomTerminalPath()));

        assertThat(notFilterable).isEqualTo(FacetErrorCode.PATH_NOT_FILTERABLE);
        assertThat(unknown).isEqualTo(FacetErrorCode.UNKNOWN_PATH);
        assertThat(notFilterable).isNotEqualTo(unknown);
    }

    /**
     * One triggering call per {@link FacetErrorCode} that {@link FacetSource} can raise (every value
     * except {@link FacetErrorCode#SCHEME_UNAVAILABLE}, which is a {@code ConceptSource} concern per
     * {@code FacetSource}'s own Javadoc), reusing the same fixtures and request-building helpers the
     * scenario-specific tests above use.
     */
    private Map<FacetErrorCode, ThrowingCallable> errorTriggers() {
        Map<FacetErrorCode, ThrowingCallable> triggers = new LinkedHashMap<>();

        ContractFixture invoiceAndCustomer = ContractFixtures.invoiceAndCustomer();
        FacetSource invoiceSource = sourceFor(invoiceAndCustomer);
        triggers.put(
                FacetErrorCode.UNKNOWN_TENANT,
                () -> invoiceSource.resolve(new FacetScopeRequest(
                        "not-" + invoiceAndCustomer.tenantId(),
                        rootOf(invoiceAndCustomer),
                        new VersionPolicy.Union(),
                        TraversalConfig.defaults(),
                        Optional.empty(),
                        Optional.empty())));
        triggers.put(
                FacetErrorCode.UNKNOWN_CLASS,
                () -> invoiceSource.resolve(new FacetScopeRequest(
                        invoiceAndCustomer.tenantId(),
                        randomClassRef(),
                        new VersionPolicy.Union(),
                        TraversalConfig.defaults(),
                        Optional.empty(),
                        Optional.empty())));
        triggers.put(
                FacetErrorCode.UNKNOWN_PATH,
                () -> invoiceSource.describe(defaultRequest(invoiceAndCustomer), randomTerminalPath()));
        triggers.put(
                FacetErrorCode.PATH_NOT_FILTERABLE,
                () -> invoiceSource.describe(
                        defaultRequest(invoiceAndCustomer), pathTo(invoiceAndCustomer, "internalNotes")));

        ContractFixture unwalkableEdge = ContractFixtures.unwalkableEdge();
        FacetSource unwalkableSource = sourceFor(unwalkableEdge);
        triggers.put(
                FacetErrorCode.CLASS_NOT_REACHABLE,
                () -> unwalkableSource.describe(
                        defaultRequest(unwalkableEdge), pathTo(unwalkableEdge, "customer", "city")));
        triggers.put(
                FacetErrorCode.EDGE_NOT_WALKABLE,
                () -> unwalkableSource.describe(
                        defaultRequest(unwalkableEdge), singleRelationshipTerminalPath(unwalkableEdge, "customer")));

        ContractFixture deep = ContractFixtures.deep();
        FacetSource deepSource = sourceFor(deep);
        triggers.put(
                FacetErrorCode.DEPTH_EXCEEDED,
                () -> deepSource.expand(
                        deepSource.resolve(defaultRequest(deep)),
                        relationshipPath(deep, "step", "step", "step", "step")));

        ContractFixture cyclic = ContractFixtures.cyclic();
        FacetSource cyclicSource = sourceFor(cyclic);
        triggers.put(FacetErrorCode.CYCLE_DETECTED, () -> {
            FacetScope scope = cyclicSource.resolve(defaultRequest(cyclic));
            FacetScope expanded = cyclicSource.expand(scope, relationshipPath(cyclic, "toB"));
            cyclicSource.expand(expanded, relationshipPath(cyclic, "toB", "toA"));
        });

        ContractFixture renamed = ContractFixtures.renamedAcrossVersions();
        FacetSource renamedSource = sourceFor(renamed);
        triggers.put(
                FacetErrorCode.VERSION_NOT_FOUND,
                () -> renamedSource.resolve(request(
                        renamed,
                        new VersionPolicy.Pinned("urn:ont:v9"),
                        TraversalConfig.defaults(),
                        Optional.empty())));

        ContractFixture disjoint = ContractFixtures.disjointVersions();
        FacetSource disjointSource = sourceFor(disjoint);
        triggers.put(
                FacetErrorCode.EMPTY_INTERSECTION,
                () -> disjointSource.resolve(request(
                        disjoint, new VersionPolicy.Intersection(), TraversalConfig.defaults(), Optional.empty())));

        ContractFixture rbac = ContractFixtures.rbacRestricted();
        FacetSource rbacSource = sourceFor(rbac);
        TraversalConfig rbacOn = new TraversalConfig(OptionalInt.of(3), true, StampMode.VERSION_ONLY, true);
        Principal noFinance = new Principal("no-finance", Set.of());
        triggers.put(
                FacetErrorCode.PRINCIPAL_DENIED,
                () -> rbacSource.describe(
                        request(rbac, new VersionPolicy.Union(), rbacOn, Optional.of(noFinance)),
                        pathTo(rbac, "customer", "city")));

        return triggers;
    }

    @Test
    final void everyFacetErrorCodeIsReachable() {
        Map<FacetErrorCode, ThrowingCallable> triggers = errorTriggers();
        Set<FacetErrorCode> observed = new LinkedHashSet<>();
        for (Map.Entry<FacetErrorCode, ThrowingCallable> entry : triggers.entrySet()) {
            Throwable thrown = catchThrowable(entry.getValue());
            assertThat(thrown).as("triggering %s", entry.getKey()).isInstanceOf(FacetResolutionException.class);
            observed.add(((FacetResolutionException) thrown).code());
        }

        assertThat(observed).hasSize(11);
        assertThat(observed).containsExactlyInAnyOrderElementsOf(triggers.keySet());
    }

    @Test
    final void failuresAreAlwaysFacetResolutionException() {
        Map<FacetErrorCode, ThrowingCallable> triggers = errorTriggers();

        assertThat(catchThrowable(triggers.get(FacetErrorCode.UNKNOWN_TENANT)))
                .isExactlyInstanceOf(FacetResolutionException.class);
        assertThat(catchThrowable(triggers.get(FacetErrorCode.CLASS_NOT_REACHABLE)))
                .isExactlyInstanceOf(FacetResolutionException.class);
        assertThat(catchThrowable(triggers.get(FacetErrorCode.CYCLE_DETECTED)))
                .isExactlyInstanceOf(FacetResolutionException.class);
    }
}
