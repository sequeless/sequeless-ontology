package org.sequeless.ontology.facet.core.api;

import java.util.Objects;
import java.util.UUID;

/**
 * A reference to an ontology class.
 *
 * <p>Per design.md section 2.1, classes and properties carry an immutable {@link UUID} distinct
 * from their slug and label. The UUID is identity; the slug is presentation. This is what lets a
 * class be renamed without breaking anything that referenced it: a {@link FacetPath} persists as a
 * list of UUIDs, and the slug shown alongside it is simply whatever is current at render time.
 *
 * @param id the durable identity of the class; never {@code null} and never reused
 * @param slug the current short name used when rendering a path as text (for example
 *     {@code "customer"} in {@code "customer.city"}); never {@code null} or blank
 * @param iri the class's IRI in the source ontology; never {@code null} or blank
 * @param label the current human-facing display name; never {@code null}
 */
public record ClassRef(UUID id, String slug, String iri, LocalizedText label) {

    /** Validates identity, presentation, and label are all present and non-blank where required. */
    public ClassRef {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(slug, "slug must not be null");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        Objects.requireNonNull(iri, "iri must not be null");
        if (iri.isBlank()) {
            throw new IllegalArgumentException("iri must not be blank");
        }
        Objects.requireNonNull(label, "label must not be null");
    }
}
