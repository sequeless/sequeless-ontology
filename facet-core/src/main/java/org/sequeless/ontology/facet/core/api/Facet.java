package org.sequeless.ontology.facet.core.api;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A single queryable property at the end of a {@link FacetPath}: its type, what may be done with
 * it, and everything a consumer such as {@code sequeless-filter} needs in order to build a filter
 * UI and validate expressions without knowing anything else about the ontology.
 *
 * <p>Per design.md section 2.4, this is the central model type of the facet contract — the thing
 * a {@code FacetScopeRequest} resolves to, one entry per property.
 *
 * <p><b>{@code Optional} record components.</b> {@link #conceptScheme()} and {@link #conflict()}
 * are typed as {@code Optional} rather than nullable, because design.md section 2.4 specifies them
 * that way literally, and design.md wins over the general convention against {@code Optional} as a
 * field type. That convention exists to protect mutable beans and serialization frameworks, and
 * neither applies here: this contract's types are immutable records, and wire serialization is
 * explicitly out of scope (design.md section 6). Record {@code equals}/{@code hashCode} work
 * correctly with {@code Optional} components regardless, because {@code Optional} is itself
 * value-based.
 *
 * @param path the location of this facet, relative to whatever root the request resolved against;
 *     never {@code null}
 * @param type the closed value shape of this facet; never {@code null}
 * @param nativeDatatypeIri the facet's datatype IRI in the source ontology, carried alongside
 *     {@link #type()} for full fidelity; may be {@code null} when the ontology does not expose one
 * @param cardinality whether a record holds at most one value or potentially several for this
 *     facet; never {@code null}
 * @param capabilities what may be done with this facet's values — filter, facet, search, sort;
 *     never {@code null}
 * @param label the current human-facing display name of this facet; never {@code null}
 * @param description a longer human-facing explanation of this facet; may be {@code null} when the
 *     ontology does not provide one
 * @param conceptScheme the concept scheme this facet's values are drawn from, present only for
 *     {@code CONCEPT_REF}-typed facets; never {@code null} as a wrapper — use {@link
 *     Optional#empty()} rather than {@code null} when there is no scheme
 * @param restriction the ontology's narrowing of the operators a consumer would otherwise derive
 *     for this facet; never {@code null}
 * @param inference whether this facet's data may include inferred statements, or only asserted
 *     ones; never {@code null}
 * @param definedInVersions the ontology version IRIs that define this facet; never {@code null}
 *     and never empty — a facet must be defined in at least one version to exist at all
 * @param conflict present when the versions in scope disagree on this facet's type or cardinality;
 *     never {@code null} as a wrapper — use {@link Optional#empty()} rather than {@code null} when
 *     there is no conflict
 */
public record Facet(
        FacetPath path,
        FacetType type,
        String nativeDatatypeIri,
        Cardinality cardinality,
        Capabilities capabilities,
        LocalizedText label,
        LocalizedText description,
        Optional<ConceptSchemeRef> conceptScheme,
        OperatorRestriction restriction,
        InferenceSupport inference,
        Set<String> definedInVersions,
        Optional<FacetConflict> conflict) {

    /**
     * Validates every required field is present, defensively copies {@code definedInVersions}
     * (which also null-checks the set and every element) and rejects it being empty, and requires
     * the {@code Optional} wrappers themselves to be non-{@code null} (their contents may of
     * course be absent).
     */
    public Facet {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(type, "type must not be null");
        // nativeDatatypeIri may be null: not every ontology exposes one.
        Objects.requireNonNull(cardinality, "cardinality must not be null");
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        Objects.requireNonNull(label, "label must not be null");
        // description may be null: the ontology may not provide one.
        Objects.requireNonNull(conceptScheme, "conceptScheme must not be null (use Optional.empty())");
        Objects.requireNonNull(restriction, "restriction must not be null");
        Objects.requireNonNull(inference, "inference must not be null");
        definedInVersions = Set.copyOf(definedInVersions); // null-checks the set and every element
        if (definedInVersions.isEmpty()) {
            throw new IllegalArgumentException("definedInVersions must not be empty");
        }
        Objects.requireNonNull(conflict, "conflict must not be null (use Optional.empty())");
    }
}
