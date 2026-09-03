package org.sequeless.ontology.facet.core.api;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The result of resolving a {@link FacetScopeRequest}: the root's own facets, the relationships
 * walkable from the current frontier, and enough bookkeeping to re-expand or cache the scope.
 *
 * <p>Per design.md section 2.6 and section 4.2, {@code facets} is populated <b>eagerly at depth 1
 * and lazily beyond</b> — {@code resolve} returns the root's own facets fully populated, plus the
 * walkable relationships, which is enough to render a complete filter builder for the common case
 * with one call. Going deeper means calling {@code FacetSource.expand} with one of {@link
 * #relationships()} to obtain a further {@code FacetScope}.
 *
 * <p>This is not an optimization; it is forced. Under unbounded traversal, a flat list of every
 * legal path is infinite the moment a cycle exists, and even in an acyclic graph the list is
 * exponential in fan-out — 30 properties &times; 5 relationships &times; 30 properties is 4 500
 * entries at depth 2 alone. Eager depth-1 plus lazy expansion is the only shape that stays bounded
 * regardless of how the ontology graph is connected.
 *
 * @param root the class this scope is rooted at, echoed from the resolving request; never {@code
 *     null}
 * @param facets the facets available at the current frontier, fully populated; never {@code null},
 *     may be empty
 * @param relationships the walkable edges from the current frontier — following one via {@code
 *     FacetSource.expand} extends this scope one hop further; never {@code null}, may be empty
 * @param stamp this scope's freshness stamp, composed as described by {@link
 *     TraversalConfig#stampMode()}; never {@code null} or blank. Per design.md section 4.9, this
 *     field is <b>always present</b>, and only its composition varies with the stamp mode. If it
 *     were absent under the cheap mode, tightening a deployment to {@code CONTENT_HASH} stamps
 *     later would be a breaking change, and saved searches written in the meantime would have
 *     nothing recorded to compare against.
 * @param ontologyVersions the ontology version IRIs that contributed to this scope; never {@code
 *     null}, may be empty only if no version-defined content is in scope
 * @param request the request this scope was resolved from, echoed back so the scope can be
 *     re-expanded (for example by {@code FacetSource.expand}) without the caller having to
 *     reassemble it; never {@code null}
 */
public record FacetScope(
        ClassRef root,
        List<Facet> facets,
        List<Relationship> relationships,
        String stamp,
        Set<String> ontologyVersions,
        FacetScopeRequest request) {

    /**
     * Validates {@code root}, {@code stamp}, and {@code request} are present, defensively copies
     * {@code facets} and {@code relationships} (which also null-checks each list and every
     * element), defensively copies {@code ontologyVersions} (which also null-checks the set and
     * every element), and rejects a blank {@code stamp}.
     */
    public FacetScope {
        Objects.requireNonNull(root, "root must not be null");
        facets = List.copyOf(facets); // null-checks the list and every element
        relationships = List.copyOf(relationships); // null-checks the list and every element
        Objects.requireNonNull(stamp, "stamp must not be null");
        if (stamp.isBlank()) {
            throw new IllegalArgumentException("stamp must not be blank");
        }
        ontologyVersions = Set.copyOf(ontologyVersions); // null-checks the set and every element
        Objects.requireNonNull(request, "request must not be null");
    }
}
