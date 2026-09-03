package org.sequeless.ontology.facet.core.api;

import java.util.Objects;
import java.util.Set;

/**
 * The identity and roles of whoever is asking for a facet scope, used only when
 * {@link TraversalConfig#rbacFiltersFacets()} is enabled.
 *
 * <p>Per design.md section 4.10, RBAC facet filtering is optional and off by default: the row-level
 * predicate does the real access-control work, and everyone sees the same schema. When a caller
 * opts in, resolution takes a {@code Principal} and paths reaching a class the principal cannot
 * read are simply absent from the result, with typing one raising {@code PRINCIPAL_DENIED}. This
 * prevents the facet list itself from disclosing schema the principal should not see, and prevents
 * users from building filters that silently return nothing because the underlying class is denied.
 *
 * @param id the durable identity of the requesting principal; never {@code null} or blank
 * @param roles the roles held by this principal, used by the ontology to decide which facets and
 *     relationships are visible; never {@code null}, may be empty
 */
public record Principal(String id, Set<String> roles) {

    /**
     * Validates {@code id} is present and non-blank, and defensively copies {@code roles} (which
     * also null-checks the set and every element).
     */
    public Principal {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        roles = Set.copyOf(roles); // null-checks the set and every element
    }
}
