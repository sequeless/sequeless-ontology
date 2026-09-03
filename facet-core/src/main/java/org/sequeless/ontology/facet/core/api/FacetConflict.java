package org.sequeless.ontology.facet.core.api;

import java.util.List;
import java.util.Objects;

/**
 * The record of a merge conflict on a single facet: the incompatible per-version definitions, and
 * why they conflict.
 *
 * <p>Per design.md section 4.5, when the versions in scope disagree on a property's {@link
 * FacetType} or {@link Cardinality}, the facet is <b>kept and marked</b> with a {@code
 * FacetConflict} — it is never dropped and the resolution never fails outright. The rejected
 * alternatives are both worse:
 *
 * <ul>
 *   <li><b>Failing the whole resolution</b> would let one bad property change across versions
 *       render an entire class unqueryable, in a system whose premise is that versions coexist.
 *   <li><b>Last-version-wins</b> is silently incorrect: a {@code DATETIME} comparison run against
 *       an earlier version's string data either errors deep in the backend or quietly matches
 *       nothing, and the caller has no way to know that happened.
 * </ul>
 *
 * <p>Instead, a conflicted {@link Facet} is returned with a widest-common {@link Facet#type()} and
 * this {@code FacetConflict} attached, and the query still runs and still returns records from
 * every version. Consumers are expected to offer only operators valid for <b>every</b> conflicting
 * definition — for the {@code issuedAt} example in design.md section 4.5 ({@code STRING} in v1,
 * {@code DATETIME} in v2), that is {@code is}, {@code is not}, {@code is in}, {@code exists}, and
 * {@code does not exist}, but not {@code >} or {@code between}.
 *
 * @param definitions every conflicting per-version definition of the facet; never {@code null},
 *     never empty, and never containing {@code null} elements — a conflict is meaningless without
 *     at least one alternative definition to conflict with
 * @param reason a short, human-facing explanation of what changed (for example {@code "datatype
 *     changed between versions"}); never {@code null} or blank
 */
public record FacetConflict(List<ConflictingDefinition> definitions, String reason) {

    /**
     * Defensively copies {@code definitions} (which also null-checks the list and every element),
     * rejects an empty list, and validates {@code reason} is present.
     */
    public FacetConflict {
        definitions = List.copyOf(definitions); // null-checks the list and every element
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("definitions must not be empty");
        }
        Objects.requireNonNull(reason, "reason must not be null");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }
}
