package org.sequeless.ontology.facet.core.api;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A walkable edge from one class to another: a property whose value is a reference to an instance
 * of {@link #targetClass()} rather than a literal.
 *
 * <p>Per design.md section 2.5, only relationships the ontology has marked <b>walkable</b> are
 * ever returned to a caller — an unmarked relationship is invisible to traversal. This is the
 * primary guard against path explosion (design.md section 4.3): a class with hundreds of
 * reference-valued properties does not silently expand a caller's scope through every one of them,
 * only through the ones the ontology author has deliberately opened up.
 *
 * <p>Walkability is therefore not represented as a field on this type. There is no {@code
 * walkable} boolean to check, because an edge the ontology has not marked walkable simply never
 * becomes a {@code Relationship} in the first place — it is filtered out before it ever reaches
 * this contract's callers.
 *
 * @param propertyId the durable identity of the property that carries this relationship; never
 *     {@code null}
 * @param slug the current short name of the property, used for presentation; never {@code null} or
 *     blank
 * @param label the current human-facing display name of the relationship; never {@code null}
 * @param targetClass the class an instance of this relationship refers to; never {@code null}
 * @param cardinality whether a given record holds at most one such reference or potentially
 *     several; never {@code null}
 * @param definedInVersions the ontology version IRIs that define this relationship; never {@code
 *     null} and never empty — a relationship must be defined in at least one version to exist at
 *     all
 */
public record Relationship(
        UUID propertyId,
        String slug,
        LocalizedText label,
        ClassRef targetClass,
        Cardinality cardinality,
        Set<String> definedInVersions) {

    /**
     * Validates identity, presentation, and typing fields are present, and defensively copies
     * {@code definedInVersions} (which also null-checks the set and every element), rejecting an
     * empty set.
     */
    public Relationship {
        Objects.requireNonNull(propertyId, "propertyId must not be null");
        Objects.requireNonNull(slug, "slug must not be null");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(targetClass, "targetClass must not be null");
        Objects.requireNonNull(cardinality, "cardinality must not be null");
        definedInVersions = Set.copyOf(definedInVersions); // null-checks the set and every element
        if (definedInVersions.isEmpty()) {
            throw new IllegalArgumentException("definedInVersions must not be empty");
        }
    }
}
