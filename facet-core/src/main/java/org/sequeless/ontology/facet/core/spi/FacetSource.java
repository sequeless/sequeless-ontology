package org.sequeless.ontology.facet.core.spi;

import org.sequeless.ontology.facet.core.api.Capabilities;
import org.sequeless.ontology.facet.core.api.ClassRef;
import org.sequeless.ontology.facet.core.api.Facet;
import org.sequeless.ontology.facet.core.api.FacetErrorCode;
import org.sequeless.ontology.facet.core.api.FacetPath;
import org.sequeless.ontology.facet.core.api.FacetResolutionException;
import org.sequeless.ontology.facet.core.api.FacetScope;
import org.sequeless.ontology.facet.core.api.FacetScopeRequest;
import org.sequeless.ontology.facet.core.api.PathSegment;
import org.sequeless.ontology.facet.core.api.Relationship;
import org.sequeless.ontology.facet.core.api.VersionPolicy;

/**
 * The driven port a schema-backed adapter implements to answer what may be filtered for a given
 * tenant, root class, and version policy.
 *
 * <p>Per design.md section 3.1, this contract is three methods rather than one, because {@link
 * #resolve(FacetScopeRequest) resolve}, {@link #expand(FacetScope, FacetPath) expand}, and {@link
 * #describe(FacetScopeRequest, FacetPath) describe} answer three distinct questions, and
 * conflating any two of them forces the implementation to be either wasteful or incomplete:
 *
 * <ul>
 *   <li>{@code resolve} powers first paint of a filter builder — the root's own facets and
 *       walkable relationships, fully populated, in one call (design.md section 4.2).
 *   <li>{@code expand} powers "the user typed a dot after {@code customer}" — exactly one further
 *       hop, without re-walking or discarding anything already gathered.
 *   <li>{@code describe} powers re-validating a saved search without enumerating anything — it
 *       answers "does this one path still resolve, and to what", at a cost independent of how many
 *       sibling facets or relationships the classes along the path happen to have.
 * </ul>
 *
 * <p>None of these three is given a {@code default} implementation built on another, and none
 * should ever be added. A {@code default describe} built by walking {@code resolve} one hop at a
 * time and inspecting the result would re-enumerate every facet and relationship of every
 * intermediate class along the path just to answer a yes/no question about one property at the
 * end of it — exactly the cost {@code describe} exists to avoid (design.md section 3.1). Each
 * method needs its own, independently cheap, implementation.
 */
public interface FacetSource {

    /**
     * Resolve a scope rooted at {@code request.root()}: the root's own facets, fully populated,
     * and the relationships walkable from it. Nothing beyond depth 1 is returned; reach further
     * with {@link #expand(FacetScope, FacetPath)}.
     *
     * <p>Per design.md section 4.2, eager-at-depth-1-lazy-beyond is not an optimization but a
     * forced shape: a flat list of every path reachable from a root is infinite the moment the
     * ontology graph contains a cycle, and even acyclic it is exponential in fan-out — 30
     * properties &times; 5 relationships &times; 30 properties is 4 500 entries at depth 2 alone.
     *
     * <h2>Guarantees</h2>
     *
     * <ul>
     *   <li>Every facet defined directly on {@code request.root()} is present in {@link
     *       FacetScope#facets()}, merged according to {@code request.versionPolicy()} (design.md
     *       section 4.4) and carrying any {@link
     *       org.sequeless.ontology.facet.core.api.FacetConflict} the merge produced (design.md
     *       section 4.5).
     *   <li>Every relationship directly on {@code request.root()} that the ontology has marked
     *       walkable is present in {@link FacetScope#relationships()}. An edge the ontology has
     *       not marked walkable never appears — by construction, because it never becomes a {@link
     *       Relationship} in the first place, not because some filtering step removed it
     *       afterward (design.md section 2.5).
     *   <li>Nothing beyond depth 1 is included: no facet or relationship belonging to a related
     *       class, however reachable, appears in the returned scope.
     *   <li>{@link FacetScope#stamp()} is composed per {@code
     *       request.traversal().stampMode()} and is never blank (design.md section 4.9); the field
     *       itself is always present regardless of stamp mode.
     *   <li>{@link FacetScope#request()} echoes {@code request} unchanged, so the returned scope
     *       can be passed straight to {@link #expand(FacetScope, FacetPath)} without the caller
     *       reassembling the request that produced it.
     *   <li>The ordering of {@link FacetScope#facets()} and {@link FacetScope#relationships()} is
     *       implementation-chosen, but stable across repeated calls with an equal {@code request}
     *       — a caller may rely on getting the same order back without being told what that order
     *       is.
     * </ul>
     *
     * @param request who is asking, from which tenant, rooted at which class, under which version
     *     policy, traversal configuration, and (optionally) principal and locale; never {@code
     *     null}
     * @return the resolved scope; never {@code null}
     * @throws FacetResolutionException in the following cases, each identified by {@link
     *     FacetResolutionException#code()}:
     *     <ul>
     *       <li>{@link FacetErrorCode#UNKNOWN_TENANT} — {@code request.tenantId()} names no known
     *           tenant.
     *       <li>{@link FacetErrorCode#UNKNOWN_CLASS}, carrying the offending {@link ClassRef} —
     *           {@code request.root()} names no known class.
     *       <li>{@link FacetErrorCode#VERSION_NOT_FOUND} — {@code request.versionPolicy()} is a
     *           {@link VersionPolicy.Pinned} naming a version IRI that does not exist.
     *       <li>{@link FacetErrorCode#EMPTY_INTERSECTION} — {@code request.versionPolicy()} is
     *           {@link VersionPolicy.Intersection} and no facet is defined in every version in
     *           scope, so nothing survives the merge (design.md section 4.4).
     *       <li>{@link FacetErrorCode#PRINCIPAL_DENIED} — {@code
     *           request.traversal().rbacFiltersFacets()} is enabled (design.md section 4.10) and
     *           the principal named by {@code request.principal()} cannot read {@code
     *           request.root()} itself.
     *     </ul>
     *     This method never throws with {@link FacetErrorCode#UNKNOWN_PATH}, {@link
     *     FacetErrorCode#PATH_NOT_FILTERABLE}, {@link FacetErrorCode#EDGE_NOT_WALKABLE}, {@link
     *     FacetErrorCode#CYCLE_DETECTED}, {@link FacetErrorCode#DEPTH_EXCEEDED}, or {@link
     *     FacetErrorCode#SCHEME_UNAVAILABLE}: {@code request} carries neither a {@link FacetPath}
     *     to walk nor a concept scheme to reach, so none of those codes has anything to attach to.
     */
    FacetScope resolve(FacetScopeRequest request);

    /**
     * Extend an already-resolved {@code scope} one hop further, along {@code relationship}.
     *
     * <p><b>{@code relationship} is the full path from the scope's root, not a bare one-hop
     * name.</b> This is the subtlest part of this contract to get right as a caller, so it is
     * worth stating twice: a {@link Relationship} itself carries no path prefix — it is a
     * standalone description of one edge, reusable at every class it appears on — so accumulating
     * a {@link FacetPath} of {@link PathSegment}s is the only way a caller can name an edge that is
     * several hops deep. To expand {@code customer.account}, the caller passes a two-segment
     * {@code FacetPath} ({@code customer}, then {@code account}), not a one-segment path naming
     * {@code account} alone — {@code account} is not walkable directly from {@code scope.root()}.
     *
     * <p>Because {@link FacetPath} requires every non-terminal segment to carry a {@link
     * PathSegment#targetClass()}, the last segment of {@code relationship} is the only one that
     * could legally be terminal — and for this method it must not be: {@link
     * PathSegment#targetClass()} on the last segment must be non-{@code null}, because {@code
     * relationship} names a relationship to traverse into, not a terminal scalar facet.
     *
     * <h2>Guarantees</h2>
     *
     * <ul>
     *   <li>The result contains the facets and walkable relationships of the class {@code
     *       relationship} terminates in, each reachable under the accumulated path (so a facet
     *       named {@code account.status} two hops past {@code customer} appears as such, not as a
     *       bare {@code status}).
     *   <li>Everything already present in {@code scope} is preserved — {@code expand} only adds;
     *       nothing already in {@code scope.facets()} or {@code scope.relationships()} is ever
     *       removed or altered by a later call.
     *   <li>Exactly one hop of new content is added — the facets and relationships of the classes
     *       {@code relationship} passes through along the way are not included, only those of the
     *       terminal class.
     *   <li>{@code expand} is idempotent: calling it twice with an equal {@code relationship}
     *       against an equal {@code scope} yields an equal resulting scope.
     *   <li>{@link FacetScope#stamp()} on the result is unchanged from {@code scope.stamp()}.
     *       Stamp composition depends on the ontology versions in scope, the traversal
     *       configuration, the version policy, the tenant, and (in {@code CONTENT_HASH} mode) the
     *       principal — never on how much of the graph has been walked, so expanding further must
     *       not perturb it (design.md section 4.9).
     * </ul>
     *
     * @param scope the previously resolved (or previously expanded) scope to extend; never {@code
     *     null}
     * @param relationship the full path from {@code scope.root()} to the relationship to traverse,
     *     whose last segment names a relationship (non-{@code null} {@link
     *     PathSegment#targetClass()}); never {@code null}
     * @return a new scope containing everything in {@code scope} plus the terminal class's facets
     *     and walkable relationships; never {@code null}
     * @throws FacetResolutionException in the following cases, each identified by {@link
     *     FacetResolutionException#code()}:
     *     <ul>
     *       <li>{@link FacetErrorCode#UNKNOWN_PATH} — some segment of {@code relationship} names no
     *           property at that point in the walk, or the last segment names a scalar facet
     *           rather than a relationship.
     *       <li>{@link FacetErrorCode#EDGE_NOT_WALKABLE} — some segment's property exists as a
     *           relationship at that point, but the ontology has never marked it walkable (Guard 1,
     *           design.md section 4.3).
     *       <li>{@link FacetErrorCode#CYCLE_DETECTED} — {@code scope.request().traversal()
     *           .noRevisitClasses()} is enabled and some segment re-enters a class already on this
     *           path, including {@code scope.root()} itself (Guard 2, design.md section 4.3). This
     *           check is per-path, not global: a class may legitimately appear in two different
     *           paths expanded from the same scope.
     *       <li>{@link FacetErrorCode#DEPTH_EXCEEDED} — {@code scope.request().traversal()
     *           .maxDepth()} is present and {@code relationship.depth()} exceeds it (Guard 3,
     *           design.md section 4.3).
     *       <li>{@link FacetErrorCode#PRINCIPAL_DENIED} — {@code scope.request().traversal()
     *           .rbacFiltersFacets()} is enabled and the principal cannot read the class {@code
     *           relationship} terminates in.
     *     </ul>
     */
    FacetScope expand(FacetScope scope, FacetPath relationship);

    /**
     * Validate and describe exactly one {@code path} against {@code request}, without enumerating
     * anything else.
     *
     * <p>The guarantee that makes this method worth having, distinct from composing {@code
     * resolve} and {@code expand}: {@code describe} must not enumerate the facets or relationships
     * of any intermediate class along {@code path}. At each hop it checks only "does this one
     * property exist here, and may I walk it", and at the end it builds the single terminal {@link
     * Facet} — nothing about {@code path}'s siblings is ever computed. This is what makes
     * re-validating a saved search, one path at a time, cheap regardless of how large the classes
     * along the way are (design.md section 3.1).
     *
     * <p>Checks apply in two phases. First, the same request-level checks {@link
     * #resolve(FacetScopeRequest)} performs against {@code request} alone — {@link
     * FacetErrorCode#UNKNOWN_TENANT}, {@link FacetErrorCode#UNKNOWN_CLASS}, {@link
     * FacetErrorCode#VERSION_NOT_FOUND}, {@link FacetErrorCode#EMPTY_INTERSECTION} — any of which
     * can fire before {@code path} is inspected at all. Only once those pass does {@code describe}
     * walk {@code path}, applying the three traversal guards (design.md section 4.3) segment by
     * segment.
     *
     * <p><b>Error code selection by position in the path.</b> Where in {@code path} the first
     * unresolvable segment falls changes which code is thrown, because "the route to the class
     * that would hold this property doesn't exist" and "the property itself doesn't exist, or
     * exists but is unusable" are different facts (design.md section 2.7):
     *
     * <ul>
     *   <li>If the first failing segment is <b>not</b> the last segment of {@code path}, the walk
     *       failed before ever reaching the class that would hold the terminal property — the
     *       whole route is broken, not just the last step. This is reported as {@link
     *       FacetErrorCode#CLASS_NOT_REACHABLE}, carrying the {@link ClassRef} of the class the
     *       path fails to reach (the target class the terminal property would have been found on).
     *   <li>If the failure is on the <b>last</b> segment, the route up to it was fine and only the
     *       terminal step itself is the problem: {@link FacetErrorCode#UNKNOWN_PATH} when no such
     *       property exists there at all, or {@link FacetErrorCode#EDGE_NOT_WALKABLE} when the
     *       property exists as a relationship but was never marked walkable.
     *   <li>If {@code path} resolves fully — every segment names a real, walkable property, and
     *       the last segment names a real facet — but that facet's {@link
     *       Capabilities#filterable()} is {@code false}, the result is {@link
     *       FacetErrorCode#PATH_NOT_FILTERABLE}. This is deliberately distinct from {@link
     *       FacetErrorCode#UNKNOWN_PATH}: "there is no such field" and "that field exists but you
     *       may not filter on it" are different facts that deserve different UI copy and produce
     *       different bug reports (design.md section 2.7).
     *   <li>{@link FacetErrorCode#CYCLE_DETECTED} and {@link FacetErrorCode#DEPTH_EXCEEDED} apply
     *       at whichever segment first triggers Guard 2 or Guard 3, regardless of whether that
     *       segment is the last one. Likewise {@link FacetErrorCode#PRINCIPAL_DENIED}, when RBAC
     *       facet filtering is enabled and some segment along the way reaches a class the
     *       principal cannot read.
     * </ul>
     *
     * <p>This method never throws with {@link FacetErrorCode#SCHEME_UNAVAILABLE}: a concept scheme
     * being unreachable is a {@code ConceptSource} concern, not a fact {@code describe} can observe
     * about a {@link FacetPath}.
     *
     * @param request the same request that would be passed to {@link #resolve(FacetScopeRequest)}
     *     — who is asking, from which tenant, rooted at which class, under which version policy and
     *     traversal configuration; never {@code null}
     * @param path the single path to validate and describe, relative to {@code request.root()};
     *     never {@code null}
     * @return the terminal {@link Facet} that {@code path} resolves to; never {@code null}
     * @throws FacetResolutionException with one of the codes and positional rules described above
     */
    Facet describe(FacetScopeRequest request, FacetPath path);
}
