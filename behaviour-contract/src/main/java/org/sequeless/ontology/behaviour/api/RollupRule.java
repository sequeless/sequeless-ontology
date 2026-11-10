package org.sequeless.ontology.behaviour.api;

import java.util.Objects;
import java.util.Optional;
import org.sequeless.filter.api.FilterNode;
import org.sequeless.ontology.schema.api.AggregateFunction;

/**
 * A declarative {@code sq:Rollup}: derive a property's value by aggregating the objects of another
 * type that relate back to it, exactly as {@code ex:Project.openTaskCount} is computed from its
 * {@code ex:Task}s. The planner (core) resolves a {@code RollupRule} into an {@code
 * AggregateRequest} — expanding {@link #sourceTypeIri()} to its concrete
 * subtypes via the meta-model's type hierarchy, exactly as it already does for {@code browse} —
 * and issues one request per distinct rule per page, never one per object.
 *
 * <p>This record does not itself enforce {@link #function()}/{@link #ofPropertyIri()} coherence —
 * for example, it does not check that {@link #ofPropertyIri()} is present for {@link
 * AggregateFunction#SUM}, {@link AggregateFunction#MIN}, {@link AggregateFunction#MAX}, or {@link
 * AggregateFunction#AVG}, or that it is absent for {@link AggregateFunction#COUNT}. That coherence
 * check is the Jena mapper's job when parsing {@code sq:Rollup} nodes, exactly as the filter tree in {@code criteria}
 * leaves operator and value coherence to filter-core's {@code FilterValidator}.
 *
 * @param sourceTypeIri the IRI of the related type to aggregate over ({@code sq:over}); must not be
 *     blank
 * @param viaIri the IRI of the relationship property on {@code sourceTypeIri} that points back to
 *     the type this rule derives a property for ({@code sq:via}); must not be blank
 * @param function the aggregate to compute ({@code sq:function}); must not be {@code null}
 * @param ofPropertyIri the IRI of the source-side property to aggregate ({@code sq:of}), required
 *     for every {@link AggregateFunction} other than {@link AggregateFunction#COUNT}, absent for
 *     {@link AggregateFunction#COUNT}, per this record's disclaimer above; must not be {@code null}
 *     (the {@link Optional} wrapper itself, not just its contents)
 * @param criteria the filter tree narrowing which source objects are aggregated ({@code
 *     sq:filter}), already resolved to a filter-core {@code FilterNode}: empty when there is no
 *     clause, otherwise a single {@code FieldFilter} or an {@code AndFilter} of the clauses in
 *     their order; must not be {@code null} (the {@link Optional} wrapper itself, not just its
 *     contents)
 * @param materialised whether this rule's value is kept correct in {@code sq_object.props} from
 *     domain events rather than computed only on read ({@code sq:materialised}), per {@link
 *     DerivationRule#materialised()}; declared last, after every field the Jena mapper already
 *     produced before this phase, so this record's canonical constructor call sites need only one
 *     new trailing argument
 */
public record RollupRule(
        String sourceTypeIri,
        String viaIri,
        AggregateFunction function,
        Optional<String> ofPropertyIri,
        Optional<FilterNode> criteria,
        boolean materialised)
        implements DerivationRule {

    public RollupRule {
        if (sourceTypeIri == null || sourceTypeIri.isBlank()) {
            throw new IllegalArgumentException("RollupRule sourceTypeIri must not be blank");
        }
        if (viaIri == null || viaIri.isBlank()) {
            throw new IllegalArgumentException("RollupRule viaIri must not be blank");
        }
        Objects.requireNonNull(function, "function must not be null");
        Objects.requireNonNull(ofPropertyIri, "ofPropertyIri must not be null");
        Objects.requireNonNull(criteria, "criteria must not be null");
    }
}
