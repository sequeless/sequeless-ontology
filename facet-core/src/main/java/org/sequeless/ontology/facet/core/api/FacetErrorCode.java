package org.sequeless.ontology.facet.core.api;

/**
 * The closed set of reasons resolving or validating a facet path can fail.
 *
 * <p>Per design.md section 2.7, resolution never returns a partial or silently-narrowed answer —
 * it either succeeds, or fails with one of these codes so the caller can branch on it and localize
 * the failure without parsing a message string. (This enum is the vocabulary for that failure; the
 * exception type that carries it is deliberately not part of this task — it is for failures against
 * real ontology data, which does not exist yet.)
 *
 * <p>Two distinctions among these codes are easy to blur and matter most:
 *
 * <ul>
 *   <li>{@link #UNKNOWN_PATH} versus {@link #PATH_NOT_FILTERABLE} — "there is no such field" and
 *       "that field exists but you may not filter on it" are different facts. Conflating them
 *       leads to the same generic error copy for two problems a user experiences completely
 *       differently, and to bug reports that all look identical until someone reads the stack
 *       trace.
 *   <li>{@link #CLASS_NOT_REACHABLE} versus {@link #EDGE_NOT_WALKABLE} — the former is a
 *       whole-path connectivity failure: no walkable route exists from the root at all (design.md
 *       section 4.1). The latter is much narrower: one specific property exists and is a
 *       relationship, but the ontology never marked it walkable, so Guard 1 (design.md section
 *       4.3) rejects it. A path can fail one without failing the other.
 * </ul>
 */
public enum FacetErrorCode {

    /** The request's {@code tenantId} does not correspond to a known tenant. */
    UNKNOWN_TENANT,

    /** The request's root class does not exist. */
    UNKNOWN_CLASS,

    /**
     * No such property exists at this point in the path. This is distinct from {@link
     * #PATH_NOT_FILTERABLE}: this code means the field itself is not there.
     */
    UNKNOWN_PATH,

    /**
     * The path resolves to a real facet, but that facet's {@code capabilities.filterable()} is
     * {@code false}. This is distinct from {@link #UNKNOWN_PATH}: the field exists and is
     * well-defined, it simply may not be used in a filter expression.
     */
    PATH_NOT_FILTERABLE,

    /**
     * No walkable route exists from the root class to the target class at all — a whole-path
     * connectivity failure (design.md section 4.1), as opposed to one edge along an otherwise
     * viable route being unmarked (see {@link #EDGE_NOT_WALKABLE}).
     */
    CLASS_NOT_REACHABLE,

    /**
     * The named property exists and is a relationship, but the ontology has not marked it
     * walkable, so Guard 1 (design.md section 4.3) rejects traversing it. Most cycles are broken
     * simply by never marking the relationship that would close them.
     */
    EDGE_NOT_WALKABLE,

    /**
     * The path exceeds the traversal configuration's {@code maxDepth} — Guard 3 (design.md
     * section 4.3), which applies even when the edge-opt-in and no-revisit guards both pass, so
     * that a wide acyclic graph cannot explode into an unbounded facet list.
     */
    DEPTH_EXCEEDED,

    /**
     * Building this path would re-enter a class already present earlier on the same path — Guard
     * 2 (design.md section 4.3). Detected per-path, not globally: the same class may legitimately
     * appear in two different paths from the same root.
     */
    CYCLE_DETECTED,

    /** The version policy names a version IRI that does not exist (for example, {@code Pinned}). */
    VERSION_NOT_FOUND,

    /**
     * The {@code Intersection} version policy left no facets at all, because no facet is defined
     * in every version in scope (design.md section 4.4).
     */
    EMPTY_INTERSECTION,

    /**
     * The principal is not permitted to reach the class this path resolves into. Raised only when
     * {@code TraversalConfig.rbacFiltersFacets} is enabled (design.md section 4.10); with it
     * disabled, the row-level predicate does the work instead and every path resolves regardless
     * of principal.
     */
    PRINCIPAL_DENIED,

    /** The concept scheme a {@code CONCEPT_REF} facet names cannot currently be reached. */
    SCHEME_UNAVAILABLE
}
