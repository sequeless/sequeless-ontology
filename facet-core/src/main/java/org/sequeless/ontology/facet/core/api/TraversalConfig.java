package org.sequeless.ontology.facet.core.api;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * How far and how a {@code FacetScopeRequest} is allowed to traverse from its root class, and how
 * the resulting scope's freshness is stamped.
 *
 * <p>Per design.md section 4.3, unbounded traversal is supported but never free by default: this
 * type bundles the three traversal guards (edge opt-in is enforced elsewhere, by the ontology
 * marking relationships walkable; {@link #noRevisitClasses()} and {@link #maxDepth()} are
 * configured here) plus the two settings that determine how a resolved scope may be cached and by
 * whom.
 *
 * @param maxDepth the maximum number of hops from the root a resolution may traverse; {@link
 *     OptionalInt#empty()} means unbounded. Unbounded is a deliberate opt-in and not the default,
 *     because the three guards in design.md section 4.3 make deep traversal <b>safe, not
 *     cheap</b> — a wide, deeply connected graph can still enumerate a very large number of
 *     relationships even with cycles and unmarked edges excluded. {@link #defaults()} therefore
 *     uses a finite depth of 3.
 * @param noRevisitClasses whether a single path may re-enter a class already on that same path.
 *     This is traversal guard 2 (design.md section 4.3) and it is <b>per-path, not global</b>: it
 *     kills {@code Invoice -> customer -> invoices -> customer -> ...} within one path without
 *     forbidding a class from legitimately appearing in two different paths, such as {@code
 *     Invoice -> customer} and {@code Invoice -> lineItems -> invoice} both reaching {@code
 *     Invoice} independently. Default {@code true}.
 * @param stampMode how the resolved scope's {@link FacetScope#stamp()} is composed; never {@code
 *     null}. Default {@link StampMode#VERSION_ONLY}.
 * @param rbacFiltersFacets whether resolution should omit facets and relationships the requesting
 *     {@link Principal} cannot read, per design.md section 4.10. Default {@code false}, so that the
 *     cache key stays free of the principal — one facet list per ontology version, rather than one
 *     per role, which is a large win when the schema itself is not sensitive and only row-level
 *     data needs an RBAC predicate. <b>Caution</b> (design.md section 4.9): turning this on while
 *     {@link #stampMode()} remains {@link StampMode#VERSION_ONLY} lets a cache keyed on the stamp
 *     serve one principal's filtered facet list to another, since the stamp does not vary with the
 *     principal in that mode. Enabling {@code rbacFiltersFacets} should force {@link
 *     StampMode#CONTENT_HASH}, or the cache must key on the request rather than on the stamp alone.
 */
public record TraversalConfig(
        OptionalInt maxDepth, boolean noRevisitClasses, StampMode stampMode, boolean rbacFiltersFacets) {

    /** Validates {@code maxDepth}, when present, is at least 1, and that {@code stampMode} is present. */
    public TraversalConfig {
        Objects.requireNonNull(maxDepth, "maxDepth must not be null (use OptionalInt.empty())");
        if (maxDepth.isPresent() && maxDepth.getAsInt() < 1) {
            throw new IllegalArgumentException("maxDepth must be >= 1 when present, was " + maxDepth.getAsInt());
        }
        Objects.requireNonNull(stampMode, "stampMode must not be null");
    }

    /**
     * The recommended default configuration: a finite depth cap of 3, per-path cycle rejection
     * enabled, cheap version-only stamps, and RBAC facet filtering off.
     *
     * @return a {@code TraversalConfig} of {@code (OptionalInt.of(3), true, StampMode.VERSION_ONLY,
     *     false)}
     */
    public static TraversalConfig defaults() {
        return new TraversalConfig(OptionalInt.of(3), true, StampMode.VERSION_ONLY, false);
    }
}
