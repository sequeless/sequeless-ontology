package org.sequeless.ontology.facet.core.testfixtures;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * One class in a {@link ContractFixture} scenario: its identity, the roles (if any) that may read
 * it, and the properties and relationships defined on it.
 *
 * @param id the durable identity of this class; never {@code null}
 * @param slug the current short name of this class; never {@code null} or blank
 * @param iri the class's IRI in the source ontology; never {@code null} or blank
 * @param readableByRoles the roles permitted to read this class when RBAC facet filtering is
 *     enabled (design.md section 4.10); empty means readable by everyone. Never {@code null},
 *     defensively copied
 * @param properties this class's own scalar properties; never {@code null}, defensively copied
 * @param relationships this class's own relationships to other classes; never {@code null},
 *     defensively copied
 */
public record ClassFixture(
        UUID id,
        String slug,
        String iri,
        Set<String> readableByRoles,
        List<PropertyFixture> properties,
        List<RelationshipFixture> relationships) {

    /** Validates identity fields and defensively copies every collection. */
    public ClassFixture {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(slug, "slug must not be null");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        Objects.requireNonNull(iri, "iri must not be null");
        if (iri.isBlank()) {
            throw new IllegalArgumentException("iri must not be blank");
        }
        readableByRoles = Set.copyOf(readableByRoles); // null-checks the set and every element
        properties = List.copyOf(properties); // null-checks the list and every element
        relationships = List.copyOf(relationships); // null-checks the list and every element
    }
}
