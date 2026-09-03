package org.sequeless.ontology.facet.core.api;

import java.util.Objects;
import java.util.UUID;

/**
 * One hop of a {@link FacetPath}: the property walked, and the class it lands on.
 *
 * <p>Like {@link ClassRef}, the property is identified durably by {@link #propertyId()} with
 * {@link #slug()} carried only for presentation. See design.md section 2.1.
 *
 * @param propertyId the durable identity of the property walked at this hop; never {@code null}
 * @param slug the current short name of the property (for example {@code "city"} in
 *     {@code "customer.city"}); never {@code null} or blank
 * @param targetClass the class this hop lands on, or {@code null} when this segment ends in a
 *     literal value rather than another class — i.e. this is the terminal segment of a path that
 *     resolves to a scalar facet rather than a further traversal
 */
public record PathSegment(UUID propertyId, String slug, ClassRef targetClass) {

    /**
     * Validates identity and presentation of the property. {@code targetClass} is intentionally
     * not validated for nullness: {@code null} is a meaningful value meaning "this segment ends in
     * a literal value" (see {@link #targetClass()}).
     */
    public PathSegment {
        Objects.requireNonNull(propertyId, "propertyId must not be null");
        Objects.requireNonNull(slug, "slug must not be null");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        // targetClass may be null: it means this segment ends in a literal value.
    }
}
