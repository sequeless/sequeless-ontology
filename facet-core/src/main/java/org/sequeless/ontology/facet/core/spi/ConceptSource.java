package org.sequeless.ontology.facet.core.spi;

import java.util.List;
import java.util.Optional;
import org.sequeless.ontology.facet.core.api.Concept;
import org.sequeless.ontology.facet.core.api.ConceptQuery;
import org.sequeless.ontology.facet.core.api.Facet;
import org.sequeless.ontology.facet.core.api.FacetErrorCode;
import org.sequeless.ontology.facet.core.api.FacetResolutionException;

/**
 * The driven port a schema-backed adapter implements to serve values for {@code CONCEPT_REF}-typed
 * facets, either by search-as-you-type or by tree traversal.
 *
 * <p>This is a separate port from {@link FacetSource}, not a pair of extra methods bolted onto it,
 * because concept schemes are published on their own clock (design.md section 4.7). A concept
 * scheme is a controlled vocabulary — a taxonomy of categories, say — that its owner can grow
 * independently of any ontology release: a new concept becomes filterable the moment the scheme
 * publishes it, no ontology deployment required. Bundling concept values into a {@link Facet}
 * response would mean the facet source would have to cache a taxonomy snapshot alongside the
 * schema and serve it back on every resolution — stale the moment the scheme changes, and coupling
 * two things that change at different rates for no benefit.
 *
 * <p>Unlike {@link FacetSource}, this port takes no {@code tenantId}. Per design.md section 3.2
 * and section 4.7, concept schemes are named by a scheme id that is not scoped to a tenant —
 * concept schemes are shared vocabulary, not tenant-owned schema, so there is nothing to require
 * here that a caller could not already omit.
 */
public interface ConceptSource {

    /**
     * List concepts in the scheme named by {@code schemeId} that match {@code query}.
     *
     * <p>Per design.md section 3.2, a concept picker operates in one of two modes, both served by
     * this one method sharing one query type:
     *
     * <ul>
     *   <li><b>Search-as-you-type</b> — {@code query.parentIri()} is absent. Results are concepts
     *       whose preferred label or any alternative label matches {@code query.partialText()}.
     *       When {@code query.partialText()} is {@code null}, this means "no text filter" and the
     *       match is unconstrained by label.
     *   <li><b>Tree-picker</b> — {@code query.parentIri()} is present. Results are exactly the
     *       <em>direct</em> children of that concept — concepts whose {@link Concept#broaderIri()}
     *       equals {@code query.parentIri()}'s value — not descendants at any depth. {@code
     *       query.partialText()} may still narrow those children by label.
     * </ul>
     *
     * <p>The returned list never exceeds {@code query.limit()} entries, since a scheme may hold
     * hundreds of thousands of concepts and a picker always asks for a page rather than the whole
     * taxonomy. Every returned {@link Concept} carries an accurate {@link Concept#hasNarrower()} —
     * that is the promise that lets a tree picker draw an expand arrow next to a node without a
     * second round trip just to discover whether it is a leaf (design.md section 3.2). Ordering of
     * the returned list is implementation-chosen.
     *
     * @param schemeId the stable id of the concept scheme to search; never {@code null} or blank
     * @param query what to search for — free text, an optional parent to list children of, an
     *     optional locale preference, and a result limit; never {@code null}
     * @return the matching concepts, up to {@code query.limit()} of them; never {@code null}, may
     *     be empty
     * @throws FacetResolutionException with {@link FacetErrorCode#SCHEME_UNAVAILABLE} when {@code
     *     schemeId} names no scheme this source can currently reach. No other {@link
     *     FacetErrorCode} applies to this method.
     */
    List<Concept> concepts(String schemeId, ConceptQuery query);

    /**
     * Look up exactly one concept by its IRI within the scheme named by {@code schemeId}.
     *
     * <p>An IRI not present in the scheme is reported as {@link Optional#empty()}, never as a
     * thrown exception. Per design.md section 4.7, a record can carry a filter value referencing a
     * concept that has since been removed from its scheme; that record's data has not changed, and
     * the value still filters correctly by IRI. This method simply cannot describe that concept
     * anymore — it cannot supply a label or a {@code hasNarrower} answer for something the scheme
     * no longer knows about — and an absent {@code Optional} is the correct, unexceptional way to
     * say so. Throwing here would treat an entirely ordinary state of a live, independently-evolving
     * vocabulary as an error.
     *
     * @param schemeId the stable id of the concept scheme to look in; never {@code null} or blank
     * @param conceptIri the IRI of the concept to look up; never {@code null} or blank
     * @return the concept, or {@link Optional#empty()} when {@code conceptIri} does not currently
     *     resolve in the scheme
     * @throws FacetResolutionException with {@link FacetErrorCode#SCHEME_UNAVAILABLE} when {@code
     *     schemeId} names no scheme this source can currently reach. No other {@link
     *     FacetErrorCode} applies to this method.
     */
    Optional<Concept> concept(String schemeId, String conceptIri);
}
