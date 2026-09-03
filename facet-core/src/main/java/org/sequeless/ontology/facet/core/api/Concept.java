package org.sequeless.ontology.facet.core.api;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One value drawn from a concept scheme — the thing a {@code CONCEPT_REF}-typed {@link Facet}
 * filters against.
 *
 * <p>Concepts live behind their own port ({@code ConceptSource}) rather than inside {@link Facet},
 * because concept schemes are published on their own clock (design.md section 4.7). Bundling
 * concept values into a facet response would mean caching a taxonomy snapshot inside that response
 * and serving it stale.
 *
 * <p>{@link #hasNarrower()} exists so a tree picker can render an expand arrow <em>without a second
 * round trip</em>. Without it, every node in a rendered tree would need its own follow-up query
 * just to discover whether it is a leaf.
 *
 * @param iri the concept's stable identifier; never {@code null} or blank. A record referencing a
 *     concept since removed from its scheme still filters by this IRI — it simply no longer
 *     autocompletes, which is correct, because the record's data has not changed
 * @param prefLabel the preferred human-facing label; never {@code null}
 * @param altLabels alternative labels, for matching what a user might type; never {@code null},
 *     possibly empty
 * @param broaderIri the parent concept in a hierarchical scheme; never {@code null} as a wrapper —
 *     use {@link Optional#empty()} for a top-level concept or a flat scheme
 * @param hasNarrower whether this concept has children, so a tree picker can render an expand
 *     arrow without asking again
 */
public record Concept(
        String iri,
        LocalizedText prefLabel,
        List<LocalizedText> altLabels,
        Optional<String> broaderIri,
        boolean hasNarrower) {

    /** Validates identity and labels, and defensively copies {@code altLabels}. */
    public Concept {
        Objects.requireNonNull(iri, "iri must not be null");
        if (iri.isBlank()) {
            throw new IllegalArgumentException("iri must not be blank");
        }
        Objects.requireNonNull(prefLabel, "prefLabel must not be null");
        altLabels = List.copyOf(altLabels);
        Objects.requireNonNull(broaderIri, "broaderIri must not be null (use Optional.empty())");
    }
}
