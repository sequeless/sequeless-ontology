package org.sequeless.ontology.facet.core.api;

import java.util.List;

/**
 * An explicit allow-list and/or deny-list of filter operator names for a facet.
 *
 * <p>Per design.md section 2.4, this is how the ontology <b>narrows</b> the set of operators a
 * consumer such as {@code sequeless-filter} would otherwise derive on its own from a facet's
 * {@link FacetType} and {@link Cardinality} (see design.md section 5, the non-normative operator
 * derivation reference). It can never widen that set — an {@code OperatorRestriction} is only ever
 * a further constraint, never a grant of an operator the type system would not otherwise support.
 *
 * <p>Both lists empty is the common case and means "no opinion": the ontology defers entirely to
 * whatever {@code sequeless-filter} would derive from the facet's type and cardinality. Use {@link
 * #unrestricted()} to construct that case explicitly.
 *
 * @param allowed when non-empty, the exhaustive set of operator names permitted for this facet,
 *     overriding derivation entirely; never {@code null}
 * @param denied operator names that derivation would otherwise offer but that are explicitly
 *     forbidden for this facet; never {@code null}
 */
public record OperatorRestriction(List<String> allowed, List<String> denied) {

    /** Defensively copies both lists, which also null-checks each list and every element. */
    public OperatorRestriction {
        allowed = List.copyOf(allowed); // null-checks the list and every element
        denied = List.copyOf(denied); // null-checks the list and every element
    }

    /**
     * The "no opinion" restriction: both {@link #allowed()} and {@link #denied()} are empty, so a
     * consumer should use whatever operator set it would otherwise derive from the facet's type
     * and cardinality.
     *
     * @return an {@code OperatorRestriction} with both lists empty
     */
    public static OperatorRestriction unrestricted() {
        return new OperatorRestriction(List.of(), List.of());
    }
}
