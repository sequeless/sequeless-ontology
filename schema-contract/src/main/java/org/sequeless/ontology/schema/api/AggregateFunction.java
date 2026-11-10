package org.sequeless.ontology.schema.api;

/**
 * The aggregate a {@code RollupRule} computes over its source objects, mirroring {@code
 * sq:count}/{@code sq:sum}/{@code sq:min}/{@code sq:max}/{@code sq:avg} in the {@code sq:}
 * vocabulary. Also the function requested by {@code AggregateRequest},
 * since a {@code RollupRule} is resolved into exactly one such request per rule.
 *
 * <p>Whether {@code RollupRule#ofPropertyIri()} must be present or absent for a given constant
 * (present for {@link #SUM}, {@link #MIN}, {@link #MAX}, and {@link #AVG}; absent for {@link
 * #COUNT}) is not enforced by this enum or by {@code RollupRule} itself — see {@code RollupRule}'s
 * javadoc for where that coherence check lives.
 */
public enum AggregateFunction {

    /**
     * The number of source objects matching the rule, dense in the resulting {@code
     * AggregateResult}: every requested target id appears, {@code 0} when
     * it has no matching source rows. Never paired with {@code RollupRule#ofPropertyIri()}.
     */
    COUNT,

    /**
     * The sum of {@code RollupRule#ofPropertyIri()} across matching source objects. Absent from
     * the resulting {@code AggregateResult} for a target with no matching
     * source rows, rather than defaulting to zero, so "no sources" stays distinguishable from
     * "sources totalling zero".
     */
    SUM,

    /**
     * The minimum of {@code RollupRule#ofPropertyIri()} across matching source objects. Absent
     * from the resulting {@code AggregateResult} for a target with no
     * matching source rows.
     */
    MIN,

    /**
     * The maximum of {@code RollupRule#ofPropertyIri()} across matching source objects. Absent
     * from the resulting {@code AggregateResult} for a target with no
     * matching source rows.
     */
    MAX,

    /**
     * The average of {@code RollupRule#ofPropertyIri()} across matching source objects. Absent
     * from the resulting {@code AggregateResult} for a target with no
     * matching source rows.
     */
    AVG
}
