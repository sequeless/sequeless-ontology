package org.sequeless.ontology.facet.core.testfixtures;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.sequeless.ontology.facet.core.api.Cardinality;

/**
 * One relationship on a {@link ClassFixture}, pointing at another class by slug.
 *
 * <p>Unlike {@link PropertyFixture#definedInVersion()}, {@link #definedInVersions()} here is
 * <b>plural</b>: relationships never go through {@code FacetMerger} — there is no
 * relationship-merge logic anywhere in {@code facet-core} — so one row carrying the whole set of
 * versions it is defined in is sufficient; there is no per-version-row reshaping to line up with.
 *
 * <p>{@link #walkable()} is a single flag over the whole set: a relationship in these fixtures is
 * walkable in every version it is defined in, or in none of them. Fixtures needing a relationship
 * that is walkable in one version but not another are out of scope for this simple model.
 *
 * @param id the durable identity of this relationship's property; never {@code null}
 * @param slug the current short name of this relationship; never {@code null} or blank
 * @param targetClassSlug the slug of the class this relationship points to; never {@code null} or
 *     blank
 * @param cardinality this relationship's cardinality; never {@code null}
 * @param walkable whether this relationship is marked walkable, per design.md section 2.5
 * @param definedInVersions every ontology version IRI this relationship is defined in; never
 *     {@code null}, defensively copied, never empty
 */
public record RelationshipFixture(
        UUID id,
        String slug,
        String targetClassSlug,
        Cardinality cardinality,
        boolean walkable,
        Set<String> definedInVersions) {

    /**
     * Validates identity fields, defensively copies {@code definedInVersions} (which also
     * null-checks the set and every element), and rejects it being empty.
     */
    public RelationshipFixture {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(slug, "slug must not be null");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        Objects.requireNonNull(targetClassSlug, "targetClassSlug must not be null");
        if (targetClassSlug.isBlank()) {
            throw new IllegalArgumentException("targetClassSlug must not be blank");
        }
        Objects.requireNonNull(cardinality, "cardinality must not be null");
        definedInVersions = Set.copyOf(definedInVersions); // null-checks the set and every element
        if (definedInVersions.isEmpty()) {
            throw new IllegalArgumentException("definedInVersions must not be empty");
        }
    }
}
