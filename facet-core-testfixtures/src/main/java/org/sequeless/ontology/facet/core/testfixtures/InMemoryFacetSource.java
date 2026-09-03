package org.sequeless.ontology.facet.core.testfixtures;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.sequeless.ontology.facet.core.api.Capabilities;
import org.sequeless.ontology.facet.core.api.Cardinality;
import org.sequeless.ontology.facet.core.api.ClassRef;
import org.sequeless.ontology.facet.core.api.ConceptSchemeRef;
import org.sequeless.ontology.facet.core.api.Facet;
import org.sequeless.ontology.facet.core.api.FacetErrorCode;
import org.sequeless.ontology.facet.core.api.FacetPath;
import org.sequeless.ontology.facet.core.api.FacetResolutionException;
import org.sequeless.ontology.facet.core.api.FacetScope;
import org.sequeless.ontology.facet.core.api.FacetScopeRequest;
import org.sequeless.ontology.facet.core.api.FacetType;
import org.sequeless.ontology.facet.core.api.InferenceSupport;
import org.sequeless.ontology.facet.core.api.LocalizedText;
import org.sequeless.ontology.facet.core.api.OperatorRestriction;
import org.sequeless.ontology.facet.core.api.PathSegment;
import org.sequeless.ontology.facet.core.api.Principal;
import org.sequeless.ontology.facet.core.api.Relationship;
import org.sequeless.ontology.facet.core.api.TraversalConfig;
import org.sequeless.ontology.facet.core.api.VersionPolicy;
import org.sequeless.ontology.facet.core.internal.FacetMerger;
import org.sequeless.ontology.facet.core.internal.StampCalculator;
import org.sequeless.ontology.facet.core.spi.FacetSource;

/**
 * An in-memory {@code FacetSource} backed directly by a {@link ContractFixture}, with a fluent
 * {@link Builder} for assembling one inline.
 *
 * <p>This is the executable specification design.md's contract test kit is proven against (see
 * {@code package-info}): every rule {@code FacetSource}'s Javadoc documents — the three traversal
 * guards of design.md section 4.3, RBAC facet filtering of design.md section 4.10, and version
 * reconciliation of design.md section 4.4 — is implemented here from the raw {@link ClassFixture},
 * {@link PropertyFixture}, and {@link RelationshipFixture} rows, delegating only the parts already
 * owned elsewhere: version-policy reconciliation to {@link FacetMerger} and stamp composition to
 * {@link StampCalculator}. Guard 1 (edge opt-in), Guard 2 (cycle rejection), and Guard 3 (depth
 * capping) belong to neither of those and are implemented here, and only here.
 *
 * <p>Java 21's exhaustive {@code switch} over {@link VersionPolicy}'s sealed permitted types
 * ({@link VersionPolicy.Union}, {@link VersionPolicy.Pinned}, {@link VersionPolicy.Intersection})
 * is used throughout this class wherever relationship-inclusion depends on the version policy —
 * the same pattern {@link FacetMerger} itself uses to reconcile facets — so that adding a fourth
 * variant to the sealed interface would fail this class's compilation rather than silently
 * miscategorizing it at runtime (design.md section 4.4).
 */
public final class InMemoryFacetSource implements FacetSource {

    private final ContractFixture fixture;

    private InMemoryFacetSource(ContractFixture fixture) {
        this.fixture = fixture;
    }

    /**
     * Wraps an already-built {@link ContractFixture} in a {@code FacetSource}.
     *
     * @param fixture the scenario to serve; never {@code null}
     * @return a source backed by {@code fixture}
     */
    public static InMemoryFacetSource from(ContractFixture fixture) {
        return new InMemoryFacetSource(fixture);
    }

    /**
     * Starts a fluent builder for assembling a {@link ContractFixture} (and the {@code
     * InMemoryFacetSource} over it) inline, without hand-constructing every {@link ClassFixture},
     * {@link PropertyFixture}, and {@link RelationshipFixture} row.
     *
     * @return a new, empty {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    // ------------------------------------------------------------------------------------------
    // FacetSource
    // ------------------------------------------------------------------------------------------

    @Override
    public FacetScope resolve(FacetScopeRequest request) {
        ValidatedRoot validated = validateRequestAndMergeRootFacets(request);
        List<Relationship> relationships = walkableRelationships(validated.classFixture(), request);
        Set<String> ontologyVersions = collectVersions(validated.mergedFacets(), relationships);
        String stamp = StampCalculator.stamp(request, ontologyVersions);
        return new FacetScope(
                request.root(), validated.mergedFacets(), relationships, stamp, ontologyVersions, request);
    }

    @Override
    public FacetScope expand(FacetScope scope, FacetPath relationship) {
        ClassFixture currentClass = findClassById(scope.root().id())
                .orElseThrow(() -> new FacetResolutionException(FacetErrorCode.UNKNOWN_CLASS, scope.root()));

        TraversalConfig traversal = scope.request().traversal();
        Set<UUID> visited = new LinkedHashSet<>();
        visited.add(scope.root().id());

        List<PathSegment> segments = relationship.segments();
        for (int i = 0; i < segments.size(); i++) {
            PathSegment segment = segments.get(i);

            RelationshipFixture relationshipFixture = findRelationship(currentClass, segment.propertyId())
                    .orElseThrow(() -> new FacetResolutionException(FacetErrorCode.UNKNOWN_PATH, relationship));
            if (!relationshipFixture.walkable()) {
                throw new FacetResolutionException(FacetErrorCode.EDGE_NOT_WALKABLE, relationship);
            }
            ClassFixture targetClass = requireTargetClass(relationshipFixture);
            if (traversal.rbacFiltersFacets()
                    && !canRead(targetClass, scope.request().principal())) {
                throw new FacetResolutionException(
                        FacetErrorCode.PRINCIPAL_DENIED, relationship, segment.targetClass());
            }
            if (traversal.noRevisitClasses()
                    && visited.contains(segment.targetClass().id())) {
                throw new FacetResolutionException(FacetErrorCode.CYCLE_DETECTED, relationship, segment.targetClass());
            }
            if (traversal.maxDepth().isPresent()
                    && (i + 1) > traversal.maxDepth().getAsInt()) {
                throw new FacetResolutionException(FacetErrorCode.DEPTH_EXCEEDED, relationship);
            }

            currentClass = targetClass;
            visited.add(currentClass.id());
        }

        List<Facet> mergedTerminalFacets = FacetMerger.merge(
                toFacets(currentClass.properties()), scope.request().versionPolicy(), fixture.versionOrder());
        List<Facet> prefixedFacets = mergedTerminalFacets.stream()
                .map(facet -> withPrefixedPath(facet, segments))
                .toList();

        List<Relationship> newRelationships = walkableRelationships(currentClass, scope.request());

        List<Facet> allFacets = new ArrayList<>(scope.facets());
        allFacets.addAll(prefixedFacets);
        List<Relationship> allRelationships = new ArrayList<>(scope.relationships());
        allRelationships.addAll(newRelationships);

        Set<String> ontologyVersions = new LinkedHashSet<>(scope.ontologyVersions());
        ontologyVersions.addAll(collectVersions(prefixedFacets, newRelationships));

        // stamp is deliberately unchanged from scope.stamp() -- see FacetSource#expand's contract.
        return new FacetScope(
                scope.root(), allFacets, allRelationships, scope.stamp(), ontologyVersions, scope.request());
    }

    @Override
    public Facet describe(FacetScopeRequest request, FacetPath path) {
        ClassFixture currentClass = validateRequestAndMergeRootFacets(request).classFixture();

        TraversalConfig traversal = request.traversal();
        Set<UUID> visited = new LinkedHashSet<>();
        visited.add(request.root().id());

        List<PathSegment> segments = path.segments();
        for (int i = 0; i < segments.size(); i++) {
            PathSegment segment = segments.get(i);
            boolean isLast = i == segments.size() - 1;

            if (!isLast) {
                currentClass = describeNonTerminalHop(currentClass, segment, path, traversal, request, visited, i);
                continue;
            }
            return describeTerminalHop(currentClass, segment, path, request);
        }
        // Unreachable: FacetPath's own compact constructor guarantees segments is never empty.
        throw new IllegalStateException("path must have at least one segment");
    }

    private ClassFixture describeNonTerminalHop(
            ClassFixture currentClass,
            PathSegment segment,
            FacetPath path,
            TraversalConfig traversal,
            FacetScopeRequest request,
            Set<UUID> visited,
            int index) {
        // Per FacetSource#describe's position-based rule: a failure to resolve the walkable
        // relationship a non-terminal segment names is reported as CLASS_NOT_REACHABLE, whether
        // no such property exists here at all or one exists but was never marked walkable -- both
        // mean the route to the class that would hold the terminal property is broken, which is a
        // different fact from the terminal-segment failures UNKNOWN_PATH/EDGE_NOT_WALKABLE cover.
        Optional<RelationshipFixture> relationshipFixture = findRelationship(currentClass, segment.propertyId());
        if (relationshipFixture.isEmpty() || !relationshipFixture.get().walkable()) {
            throw new FacetResolutionException(FacetErrorCode.CLASS_NOT_REACHABLE, path, segment.targetClass());
        }
        ClassFixture targetClass = requireTargetClass(relationshipFixture.get());
        if (traversal.rbacFiltersFacets() && !canRead(targetClass, request.principal())) {
            throw new FacetResolutionException(FacetErrorCode.PRINCIPAL_DENIED, path, segment.targetClass());
        }
        if (traversal.noRevisitClasses()
                && visited.contains(segment.targetClass().id())) {
            throw new FacetResolutionException(FacetErrorCode.CYCLE_DETECTED, path, segment.targetClass());
        }
        if (traversal.maxDepth().isPresent()
                && (index + 1) > traversal.maxDepth().getAsInt()) {
            throw new FacetResolutionException(FacetErrorCode.DEPTH_EXCEEDED, path);
        }
        visited.add(targetClass.id());
        return targetClass;
    }

    private Facet describeTerminalHop(
            ClassFixture currentClass, PathSegment segment, FacetPath path, FacetScopeRequest request) {
        List<PropertyFixture> matchingRows = currentClass.properties().stream()
                .filter(row -> row.id().equals(segment.propertyId()))
                .toList();
        if (matchingRows.isEmpty()) {
            // FacetSource#describe's Javadoc documents EDGE_NOT_WALKABLE as a possible outcome
            // when the terminal segment names a relationship the ontology never marked walkable
            // (as opposed to naming no property at all) -- distinct from ContractFixture's shape,
            // which keeps properties and relationships in separate lists, so that case is only
            // reachable by also checking the relationship list here, not the property list alone.
            Optional<RelationshipFixture> relationshipFixture = findRelationship(currentClass, segment.propertyId());
            if (relationshipFixture.isPresent() && !relationshipFixture.get().walkable()) {
                throw new FacetResolutionException(FacetErrorCode.EDGE_NOT_WALKABLE, path);
            }
            throw new FacetResolutionException(FacetErrorCode.UNKNOWN_PATH, path);
        }
        List<Facet> merged = FacetMerger.merge(toFacets(matchingRows), request.versionPolicy(), fixture.versionOrder());
        if (merged.isEmpty()) {
            // Reachable under VersionPolicy.Pinned: matchingRows share one property id, but that
            // id's sole row may belong to a version other than the pinned one, in which case
            // FacetMerger legitimately selects nothing for this group. That is exactly the
            // region-under-Pinned(v1) case design.md section 4.4's own worked example describes as
            // "typing it raises UNKNOWN_PATH" -- the property genuinely does not exist under this
            // pinned version, which is indistinguishable from it never having existed at all.
            throw new FacetResolutionException(FacetErrorCode.UNKNOWN_PATH, path);
        }
        Facet facet = merged.get(0); // exactly one group, and it survived the isEmpty() check above.
        if (!facet.capabilities().filterable()) {
            throw new FacetResolutionException(FacetErrorCode.PATH_NOT_FILTERABLE, path);
        }
        // describe validates and describes the caller's own path, so it is echoed back verbatim
        // rather than the bare single-segment path FacetMerger produced.
        return new Facet(
                path,
                facet.type(),
                facet.nativeDatatypeIri(),
                facet.cardinality(),
                facet.capabilities(),
                facet.label(),
                facet.description(),
                facet.conceptScheme(),
                facet.restriction(),
                facet.inference(),
                facet.definedInVersions(),
                facet.conflict());
    }

    // ------------------------------------------------------------------------------------------
    // Shared request-level validation (resolve + describe)
    // ------------------------------------------------------------------------------------------

    /** The outcome of the request-level validation shared by {@link #resolve} and {@link #describe}. */
    private record ValidatedRoot(ClassFixture classFixture, List<Facet> mergedFacets) {}

    /**
     * Runs every request-level check {@code resolve} and {@code describe} both perform before
     * either inspects a {@link FacetPath}: unknown tenant, unknown root class, version-policy
     * reconciliation of the root's own properties (which is where {@code VERSION_NOT_FOUND} and
     * {@code EMPTY_INTERSECTION} are raised, entirely inside {@link FacetMerger}), and RBAC on the
     * root class itself.
     */
    private ValidatedRoot validateRequestAndMergeRootFacets(FacetScopeRequest request) {
        if (!request.tenantId().equals(fixture.tenantId())) {
            throw new FacetResolutionException(FacetErrorCode.UNKNOWN_TENANT);
        }
        ClassFixture rootClass = findClassById(request.root().id())
                .orElseThrow(() -> new FacetResolutionException(FacetErrorCode.UNKNOWN_CLASS, request.root()));

        List<Facet> merged =
                FacetMerger.merge(toFacets(rootClass.properties()), request.versionPolicy(), fixture.versionOrder());

        if (request.traversal().rbacFiltersFacets() && !canRead(rootClass, request.principal())) {
            throw new FacetResolutionException(FacetErrorCode.PRINCIPAL_DENIED, request.root());
        }
        return new ValidatedRoot(rootClass, merged);
    }

    // ------------------------------------------------------------------------------------------
    // Shared lookups and conversions
    // ------------------------------------------------------------------------------------------

    private Optional<ClassFixture> findClassById(UUID id) {
        return fixture.classes().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    private Optional<ClassFixture> findClassBySlug(String slug) {
        return fixture.classes().stream().filter(c -> c.slug().equals(slug)).findFirst();
    }

    private static Optional<RelationshipFixture> findRelationship(ClassFixture classFixture, UUID propertyId) {
        return classFixture.relationships().stream()
                .filter(r -> r.id().equals(propertyId))
                .findFirst();
    }

    /**
     * Resolves a relationship's target class by slug. A dangling {@code targetClassSlug} is a
     * malformed fixture, not a domain failure a caller can branch on -- {@link ContractFixture}
     * offers no way to encode "this relationship's target does not exist" -- so it is reported as
     * {@link IllegalStateException}.
     */
    private ClassFixture requireTargetClass(RelationshipFixture relationshipFixture) {
        return findClassBySlug(relationshipFixture.targetClassSlug())
                .orElseThrow(() -> new IllegalStateException("relationship '%s' targets unknown class '%s'"
                        .formatted(relationshipFixture.slug(), relationshipFixture.targetClassSlug())));
    }

    /**
     * Converts one class's raw, one-version-at-a-time {@link PropertyFixture} rows into the
     * one-definition-per-version {@link Facet} list {@link FacetMerger#merge} requires. Each row
     * becomes a bare, single-segment {@link FacetPath}; everything else is copied straight across.
     */
    private static List<Facet> toFacets(List<PropertyFixture> rows) {
        List<Facet> facets = new ArrayList<>(rows.size());
        for (PropertyFixture row : rows) {
            FacetPath path = new FacetPath(List.of(new PathSegment(row.id(), row.slug(), null)));
            facets.add(new Facet(
                    path,
                    row.type(),
                    row.nativeDatatypeIri(),
                    row.cardinality(),
                    row.capabilities(),
                    new LocalizedText(row.slug(), null),
                    null,
                    row.conceptScheme(),
                    row.restriction(),
                    row.inference(),
                    Set.of(row.definedInVersion()),
                    Optional.empty()));
        }
        return facets;
    }

    /**
     * Whether {@code principal} may read {@code classFixture}, per design.md section 4.10: readable
     * by everyone when {@link ClassFixture#readableByRoles()} is empty, otherwise only when a
     * present principal's roles intersect it.
     */
    private static boolean canRead(ClassFixture classFixture, Optional<Principal> principal) {
        if (classFixture.readableByRoles().isEmpty()) {
            return true;
        }
        return principal
                .map(p -> !Collections.disjoint(p.roles(), classFixture.readableByRoles()))
                .orElse(false);
    }

    /**
     * The walkable relationships directly on {@code classFixture}, filtered by {@code
     * request.versionPolicy()} (Guard 1 is already built in: an unwalkable edge never reaches this
     * list) and by RBAC on each relationship's target class when {@code
     * request.traversal().rbacFiltersFacets()} is enabled. RBAC here <b>omits</b> the relationship
     * rather than throwing -- {@code resolve}/{@code expand} silently narrow the frontier; only a
     * caller actually naming a denied class in a {@link FacetPath} (via {@code expand} or {@code
     * describe}) gets {@code PRINCIPAL_DENIED}.
     */
    private List<Relationship> walkableRelationships(ClassFixture classFixture, FacetScopeRequest request) {
        Set<String> inScopeVersions = classFixture.properties().stream()
                .map(PropertyFixture::definedInVersion)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Relationship> result = new ArrayList<>();
        for (RelationshipFixture relationshipFixture : classFixture.relationships()) {
            if (!relationshipFixture.walkable()) {
                continue;
            }
            if (!includedUnderPolicy(relationshipFixture, request.versionPolicy(), inScopeVersions)) {
                continue;
            }
            ClassFixture targetClass = requireTargetClass(relationshipFixture);
            if (request.traversal().rbacFiltersFacets() && !canRead(targetClass, request.principal())) {
                continue;
            }
            result.add(toRelationship(relationshipFixture, targetClass));
        }
        return result;
    }

    /**
     * Whether a relationship survives {@code policy}, mirroring {@link FacetMerger}'s own
     * per-policy inclusion rules (design.md section 4.4) even though relationships never go
     * through {@link FacetMerger} itself (design.md section 2.5 -- there is no relationship-merge
     * logic anywhere in {@code facet-core}, only select-or-include).
     *
     * <p>{@code inScopeVersions} for {@link VersionPolicy.Intersection} is computed the same way
     * {@link FacetMerger}'s own {@code mergeIntersection} computes it: from the versions actually
     * present on the class's property rows, not from the ontology's whole global {@code
     * versionOrder} -- see that method's Javadoc for why.
     */
    private static boolean includedUnderPolicy(
            RelationshipFixture relationshipFixture, VersionPolicy policy, Set<String> inScopeVersions) {
        return switch (policy) {
            case VersionPolicy.Union ignored -> true;
            case VersionPolicy.Pinned pinned -> relationshipFixture
                    .definedInVersions()
                    .contains(pinned.versionIri());
            case VersionPolicy.Intersection ignored -> relationshipFixture
                    .definedInVersions()
                    .equals(inScopeVersions);
        };
    }

    private static ClassRef toClassRef(ClassFixture classFixture) {
        return new ClassRef(
                classFixture.id(),
                classFixture.slug(),
                classFixture.iri(),
                new LocalizedText(classFixture.slug(), null));
    }

    private static Relationship toRelationship(RelationshipFixture relationshipFixture, ClassFixture targetClass) {
        return new Relationship(
                relationshipFixture.id(),
                relationshipFixture.slug(),
                new LocalizedText(relationshipFixture.slug(), null),
                toClassRef(targetClass),
                relationshipFixture.cardinality(),
                relationshipFixture.definedInVersions());
    }

    /** Rebuilds {@code facet} with its bare single-segment path prefixed by {@code prefix}. */
    private static Facet withPrefixedPath(Facet facet, List<PathSegment> prefix) {
        PathSegment bareSegment = facet.path().segments().get(0);
        List<PathSegment> fullSegments = new ArrayList<>(prefix.size() + 1);
        fullSegments.addAll(prefix);
        fullSegments.add(new PathSegment(bareSegment.propertyId(), bareSegment.slug(), null));
        FacetPath fullPath = new FacetPath(fullSegments);
        return new Facet(
                fullPath,
                facet.type(),
                facet.nativeDatatypeIri(),
                facet.cardinality(),
                facet.capabilities(),
                facet.label(),
                facet.description(),
                facet.conceptScheme(),
                facet.restriction(),
                facet.inference(),
                facet.definedInVersions(),
                facet.conflict());
    }

    private static Set<String> collectVersions(List<Facet> facets, List<Relationship> relationships) {
        Set<String> versions = new LinkedHashSet<>();
        for (Facet facet : facets) {
            versions.addAll(facet.definedInVersions());
        }
        for (Relationship relationship : relationships) {
            versions.addAll(relationship.definedInVersions());
        }
        return versions;
    }

    // ------------------------------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------------------------------

    /**
     * A fluent, single-class builder for assembling a {@link ContractFixture} (and the {@code
     * InMemoryFacetSource} over it) inline in a test, without hand-constructing every {@link
     * ClassFixture}, {@link PropertyFixture}, and {@link RelationshipFixture} row.
     *
     * <p>Every method returns {@code this}. There are no nested or typed sub-builders and no
     * {@code .end()} call: the builder instead holds internal cursor state -- which class,
     * version, property, and relationship the chain is currently "inside" -- and each suffix
     * method (such as {@link #facetable()} or {@link #walkable()}) mutates whichever draft the
     * cursor currently points at, raising {@link IllegalStateException} if the chain calls a
     * property-only or relationship-only suffix with no matching draft active. This keeps a call
     * chain of thirty-plus fluent calls flat and linear to read, at the cost of the compiler not
     * catching a misplaced suffix -- a trade this test-fixture builder makes deliberately, since it
     * is typed by hand far more often than it is refactored (design.md section 3.1's contract test
     * kit is the primary caller).
     */
    public static final class Builder {

        private String tenantId;
        private final Map<String, ClassDraft> classesBySlug = new LinkedHashMap<>();
        private final LinkedHashSet<String> versionOrder = new LinkedHashSet<>();

        private ClassDraft currentClass;
        private String currentVersion;
        private PropertyDraft currentProperty;
        private RelationshipDraft currentRelationship;

        private Builder() {}

        /**
         * Sets the tenant every class in this fixture belongs to.
         *
         * @param tenantId the tenant id; never {@code null} or blank
         * @return {@code this}
         */
        public Builder tenant(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Opens (or resumes) a class draft. Calling this again with a slug already seen in this
         * chain resumes adding to that same class's properties and relationships rather than
         * starting a new class -- this is what lets a class's properties across several {@link
         * #version(String)} blocks be declared as separate {@code .clazz(slug).version(v)...}
         * sequences later in the same chain.
         *
         * @param slug the class's slug; never {@code null} or blank
         * @return {@code this}
         */
        public Builder clazz(String slug) {
            currentClass = classesBySlug.computeIfAbsent(slug, ClassDraft::new);
            currentVersion = null;
            currentProperty = null;
            currentRelationship = null;
            return this;
        }

        /**
         * Sets which ontology version subsequent {@link #property(String, FacetType)} and {@link
         * #relationship(String, String)} calls belong to, until the next {@link #clazz(String)} or
         * {@link #version(String)} call. The first time a given version IRI is seen anywhere in the
         * chain fixes its position in the fixture's version order (oldest first, by convention of
         * call order).
         *
         * @param versionIri the ontology version IRI; never {@code null} or blank
         * @return {@code this}
         */
        public Builder version(String versionIri) {
            requireCurrentClass();
            currentVersion = versionIri;
            versionOrder.add(versionIri);
            currentProperty = null;
            currentRelationship = null;
            return this;
        }

        /**
         * Declares one scalar property row on the current class, in the current version.
         *
         * @param slug the property's slug in this version; never {@code null} or blank
         * @param type the property's type; never {@code null}
         * @return {@code this}
         */
        public Builder property(String slug, FacetType type) {
            requireCurrentClassAndVersion();
            currentProperty = new PropertyDraft(deriveId(currentClass.slug, slug), slug, type, currentVersion);
            currentClass.properties.add(currentProperty);
            currentRelationship = null;
            return this;
        }

        /**
         * Declares one relationship row on the current class, in the current version, pointing at
         * the class named {@code targetClassSlug}.
         *
         * @param slug the relationship's slug in this version; never {@code null} or blank
         * @param targetClassSlug the slug of the class this relationship points to; never {@code
         *     null} or blank
         * @return {@code this}
         */
        public Builder relationship(String slug, String targetClassSlug) {
            requireCurrentClassAndVersion();
            currentRelationship =
                    new RelationshipDraft(deriveId(currentClass.slug, slug), slug, targetClassSlug, currentVersion);
            currentClass.relationships.add(currentRelationship);
            currentProperty = null;
            return this;
        }

        /**
         * Marks the current property facetable (bucket-and-count safe).
         *
         * @return {@code this}
         */
        public Builder facetable() {
            requireCurrentProperty("facetable()");
            currentProperty.facetable = true;
            return this;
        }

        /**
         * Marks the current property sortable.
         *
         * @return {@code this}
         */
        public Builder sortable() {
            requireCurrentProperty("sortable()");
            currentProperty.sortable = true;
            return this;
        }

        /**
         * Marks the current property searchable.
         *
         * @return {@code this}
         */
        public Builder searchable() {
            requireCurrentProperty("searchable()");
            currentProperty.searchable = true;
            return this;
        }

        /**
         * Opts the current property out of {@code filterable}, which otherwise defaults to {@code
         * true}.
         *
         * @return {@code this}
         */
        public Builder notFilterable() {
            requireCurrentProperty("notFilterable()");
            currentProperty.filterable = false;
            return this;
        }

        /**
         * Marks the current property as {@link InferenceSupport#MAY_BE_INFERRED}, which otherwise
         * defaults to {@link InferenceSupport#ASSERTED_ONLY}.
         *
         * @return {@code this}
         */
        public Builder inferred() {
            requireCurrentProperty("inferred()");
            currentProperty.inferred = true;
            return this;
        }

        /**
         * Sets the concept scheme the current property draws its values from.
         *
         * @param schemeId the scheme's stable id; never {@code null} or blank
         * @param schemeSlug the scheme's current slug; never {@code null} or blank
         * @param hierarchical whether the scheme's concepts form a hierarchy
         * @return {@code this}
         */
        public Builder conceptScheme(String schemeId, String schemeSlug, boolean hierarchical) {
            requireCurrentProperty("conceptScheme(...)");
            currentProperty.conceptScheme = new ConceptSchemeRef(schemeId, schemeSlug, hierarchical);
            return this;
        }

        /**
         * Sets the current property's native datatype IRI.
         *
         * @param iri the datatype IRI; may be {@code null}
         * @return {@code this}
         */
        public Builder nativeDatatype(String iri) {
            requireCurrentProperty("nativeDatatype(...)");
            currentProperty.nativeDatatypeIri = iri;
            return this;
        }

        /**
         * Adds to the current property's denied-operator list.
         *
         * @param operators operator names to deny; never {@code null}
         * @return {@code this}
         */
        public Builder denyOperators(String... operators) {
            requireCurrentProperty("denyOperators(...)");
            Collections.addAll(currentProperty.deniedOperators, operators);
            return this;
        }

        /**
         * Opts the current relationship into being walkable, which otherwise defaults to {@code
         * false}.
         *
         * @return {@code this}
         */
        public Builder walkable() {
            requireCurrentRelationship();
            currentRelationship.walkable = true;
            return this;
        }

        /**
         * Marks whichever of the current property or current relationship is active as {@link
         * Cardinality#MANY}, which otherwise defaults to {@link Cardinality#SINGLE}.
         *
         * @return {@code this}
         */
        public Builder many() {
            if (currentProperty != null) {
                currentProperty.cardinality = Cardinality.MANY;
            } else if (currentRelationship != null) {
                currentRelationship.cardinality = Cardinality.MANY;
            } else {
                throw new IllegalStateException("many() requires an active property or relationship");
            }
            return this;
        }

        /**
         * Overrides the deterministically derived id of whichever of the current property or
         * current relationship is active. This is how a fixture keeps one identity across a rename
         * -- call {@code .id(SAME_UUID)} on both the old-slug draft and the new-slug draft.
         *
         * @param id the id to use instead of the derived one; never {@code null}
         * @return {@code this}
         */
        public Builder id(UUID id) {
            if (currentProperty != null) {
                currentProperty.id = id;
            } else if (currentRelationship != null) {
                currentRelationship.id = id;
            } else {
                throw new IllegalStateException("id(UUID) requires an active property or relationship");
            }
            return this;
        }

        /**
         * Restricts the current class to being readable only by the given roles, per design.md
         * section 4.10. Applies regardless of whether a property or relationship is currently
         * active.
         *
         * @param roles the roles permitted to read this class; never {@code null}
         * @return {@code this}
         */
        public Builder readableBy(String... roles) {
            requireCurrentClass();
            Collections.addAll(currentClass.readableByRoles, roles);
            return this;
        }

        /**
         * Builds the {@link ContractFixture} assembled so far and wraps it in an {@code
         * InMemoryFacetSource}.
         *
         * @return the built source
         */
        public InMemoryFacetSource build() {
            return InMemoryFacetSource.from(toContractFixture());
        }

        private ContractFixture toContractFixture() {
            List<ClassFixture> classFixtures = new ArrayList<>(classesBySlug.size());
            for (ClassDraft classDraft : classesBySlug.values()) {
                List<PropertyFixture> properties = classDraft.properties.stream()
                        .map(PropertyDraft::toFixture)
                        .toList();
                List<RelationshipFixture> relationships = consolidateRelationships(classDraft.relationships);
                classFixtures.add(new ClassFixture(
                        classDraft.id,
                        classDraft.slug,
                        classDraft.iri,
                        classDraft.readableByRoles,
                        properties,
                        relationships));
            }
            return new ContractFixture(tenantId, List.copyOf(versionOrder), classFixtures);
        }

        /**
         * Consolidates every {@link RelationshipDraft} row on a class into one {@link
         * RelationshipFixture} per derived (or overridden) id, unioning the versions each row
         * declared and treating the group as walkable if any row in it opted in. This is what lets
         * the same relationship be declared once per {@link #version(String)} block -- across
         * separate {@code .clazz(slug).version(v)...} sequences later in the chain -- and still
         * collapse into the single {@code definedInVersions} set {@link RelationshipFixture}
         * requires, since (unlike {@link PropertyFixture}) it models one relationship as one row
         * carrying a whole version set, not one row per version.
         */
        private static List<RelationshipFixture> consolidateRelationships(List<RelationshipDraft> drafts) {
            Map<UUID, List<RelationshipDraft>> byId = new LinkedHashMap<>();
            for (RelationshipDraft draft : drafts) {
                byId.computeIfAbsent(draft.id, ignored -> new ArrayList<>()).add(draft);
            }
            List<RelationshipFixture> result = new ArrayList<>(byId.size());
            for (List<RelationshipDraft> group : byId.values()) {
                RelationshipDraft newest = group.get(group.size() - 1);
                Set<String> definedInVersions = new LinkedHashSet<>();
                boolean walkable = false;
                for (RelationshipDraft draft : group) {
                    definedInVersions.add(draft.definedInVersion);
                    walkable |= draft.walkable;
                }
                result.add(new RelationshipFixture(
                        newest.id,
                        newest.slug,
                        newest.targetClassSlug,
                        newest.cardinality,
                        walkable,
                        definedInVersions));
            }
            return result;
        }

        private void requireCurrentClass() {
            if (currentClass == null) {
                throw new IllegalStateException("call clazz(slug) before this method");
            }
        }

        private void requireCurrentClassAndVersion() {
            requireCurrentClass();
            if (currentVersion == null) {
                throw new IllegalStateException("call version(versionIri) before this method");
            }
        }

        private void requireCurrentProperty(String method) {
            if (currentProperty == null) {
                throw new IllegalStateException(
                        method + " requires an active property; call property(slug, type) first");
            }
        }

        private void requireCurrentRelationship() {
            if (currentRelationship == null) {
                throw new IllegalStateException(
                        "walkable() requires an active relationship; call relationship(slug, targetClassSlug) first");
            }
        }

        /**
         * Derives a deterministic, stable {@link UUID} for a class member from its owning class's
         * slug and its own slug: type-3 ({@link UUID#nameUUIDFromBytes(byte[])}), not
         * cryptographically meaningful, chosen only so two builder calls naming the same class and
         * slug agree on an id without either caller wiring one through explicitly. {@link
         * #id(UUID)} overrides this when a fixture needs one identity to survive a rename.
         */
        private static UUID deriveId(String classSlug, String slug) {
            return UUID.nameUUIDFromBytes((classSlug + "." + slug).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * One class under construction: identity, RBAC roles, and the property/relationship drafts
     * declared on it so far. A class has one identity regardless of version, so its id is derived
     * from its slug alone -- unlike a property or relationship draft, which additionally carries
     * the single version it was declared under.
     */
    private static final class ClassDraft {
        private final UUID id;
        private final String slug;
        private final String iri;
        private final Set<String> readableByRoles = new LinkedHashSet<>();
        private final List<PropertyDraft> properties = new ArrayList<>();
        private final List<RelationshipDraft> relationships = new ArrayList<>();

        private ClassDraft(String slug) {
            this.slug = slug;
            this.iri = "urn:ont:class:" + slug;
            this.id = UUID.nameUUIDFromBytes(slug.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** One version's declaration of one scalar property, mutated in place by the builder's suffix methods. */
    private static final class PropertyDraft {
        private UUID id;
        private final String slug;
        private final FacetType type;
        private final String definedInVersion;
        private boolean filterable = true;
        private boolean facetable;
        private boolean searchable;
        private boolean sortable;
        private boolean inferred;
        private Cardinality cardinality = Cardinality.SINGLE;
        private String nativeDatatypeIri;
        private ConceptSchemeRef conceptScheme;
        private final List<String> deniedOperators = new ArrayList<>();

        private PropertyDraft(UUID id, String slug, FacetType type, String definedInVersion) {
            this.id = id;
            this.slug = slug;
            this.type = type;
            this.definedInVersion = definedInVersion;
        }

        private PropertyFixture toFixture() {
            Capabilities capabilities = new Capabilities(filterable, facetable, searchable, sortable);
            InferenceSupport inference = inferred ? InferenceSupport.MAY_BE_INFERRED : InferenceSupport.ASSERTED_ONLY;
            OperatorRestriction restriction = new OperatorRestriction(List.of(), List.copyOf(deniedOperators));
            return new PropertyFixture(
                    id,
                    slug,
                    type,
                    cardinality,
                    capabilities,
                    nativeDatatypeIri,
                    Optional.ofNullable(conceptScheme),
                    restriction,
                    inference,
                    definedInVersion);
        }
    }

    /** One version's declaration of one relationship, mutated in place by the builder's suffix methods. */
    private static final class RelationshipDraft {
        private UUID id;
        private final String slug;
        private final String targetClassSlug;
        private final String definedInVersion;
        private boolean walkable;
        private Cardinality cardinality = Cardinality.SINGLE;

        private RelationshipDraft(UUID id, String slug, String targetClassSlug, String definedInVersion) {
            this.id = id;
            this.slug = slug;
            this.targetClassSlug = targetClassSlug;
            this.definedInVersion = definedInVersion;
        }
    }
}
