package org.sequeless.ontology.facet.core.api;

import java.util.Objects;

/**
 * How a {@code FacetScopeRequest} reconciles multiple ontology versions in scope into one set of
 * facets.
 *
 * <p>Per design.md section 4.4, records are stamped {@code dcterms:conformsTo <versionIRI>} and
 * never auto-upgrade, so a scope routinely spans versions — some records in scope were written
 * under v1, some under v2, and both are queried together. Deciding how to reconcile that is
 * therefore a <b>request parameter, not a deployment setting</b>: the same tenant and root class
 * may be resolved once under {@link Union} to build a filter UI that covers every record, and again
 * under {@link Pinned} to re-validate a saved search exactly as it was written.
 *
 * <p>Worked example used throughout the variant Javadoc below (design.md section 4.4): an {@code
 * Invoice} class exists in v1 and v2. Between them, the property {@code custName} was renamed to
 * {@code customerName} — same property UUID, new slug — and v2 additionally introduced a new
 * property, {@code region}.
 *
 * <p>This is a sealed interface rather than an enum because {@link Pinned} carries data (the
 * version to pin to) that the other two variants do not need.
 */
public sealed interface VersionPolicy {

    /**
     * Merge facets across every version in scope, matching by property UUID rather than by slug.
     * This is the default policy.
     *
     * <p>Because the match key is the UUID and not the slug, renaming {@code custName} to {@code
     * customerName} between v1 and v2 does not split the facet into two — it remains one {@link
     * Facet} whose {@link Facet#label()} reflects v2's current slug (the newest version's slug wins
     * for display) and whose {@link Facet#definedInVersions()} records both {@code {v1, v2}}.
     * {@code region}, defined only in v2, appears with {@code definedInVersions = {v2}}; filtering
     * on it simply matches no v1 record, since v1 records never had the property at all.
     *
     * <p>{@code Union} is the right default because it is the only policy under which a single
     * resolution covers every record in scope without silently hiding any of them.
     */
    record Union() implements VersionPolicy {}

    /**
     * Resolve against exactly one named version's schema, ignoring every other version in scope.
     *
     * <p>Under {@code Pinned("v1")} in the worked example, the facet is named {@code custName} (v1's
     * slug, not v2's), and {@code region} is entirely absent — typing {@code region} in a path
     * raises {@code UNKNOWN_PATH} because, as far as this resolution is concerned, that property
     * does not exist.
     *
     * <p>Use {@code Pinned} when a saved search must mean forever what it meant when it was
     * written: re-validating it under the version it was built against guarantees the same paths
     * resolve to the same types and the same operators, regardless of what later versions changed.
     *
     * @param versionIri the IRI of the single ontology version to resolve against; never {@code
     *     null} or blank
     */
    record Pinned(String versionIri) implements VersionPolicy {

        /** Validates {@code versionIri} is present and non-blank. */
        public Pinned {
            Objects.requireNonNull(versionIri, "versionIri must not be null");
            if (versionIri.isBlank()) {
                throw new IllegalArgumentException("versionIri must not be blank");
            }
        }
    }

    /**
     * Keep only facets defined in <b>every</b> version in scope.
     *
     * <p>In the worked example, only {@code customerName} and {@code total} survive; {@code region}
     * does not, since it is absent from v1. The guarantee this buys is stronger than {@link
     * Union}'s: every surviving filter is guaranteed to mean the same thing for every record in
     * scope, because every version defines it. If nothing survives the intersection, resolution
     * fails with {@code EMPTY_INTERSECTION} rather than returning an empty and possibly misleading
     * scope.
     */
    record Intersection() implements VersionPolicy {}
}
