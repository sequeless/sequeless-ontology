package org.sequeless.ontology.facet.core.testfixtures;

import java.util.List;
import java.util.Objects;

/**
 * A whole canned scenario: a tenant, the ontology version history in scope, and every class
 * reachable from some implicit root — enough to build an in-memory {@code FacetSource} or drive
 * the reusable contract test kit against a real one.
 *
 * <p>By convention (see {@code ContractFixtures}) {@link #classes()}' first element is the
 * scenario's root class, but this type itself does not enforce that ordering; callers that care
 * pick {@code classes().get(0)}.
 *
 * @param tenantId the tenant this scenario belongs to; never {@code null} or blank
 * @param versionOrder every ontology version IRI in this scenario, oldest first; never {@code
 *     null}, defensively copied
 * @param classes every class in this scenario; never {@code null}, defensively copied
 */
public record ContractFixture(String tenantId, List<String> versionOrder, List<ClassFixture> classes) {

    /** Validates {@code tenantId} is present and non-blank, and defensively copies both lists. */
    public ContractFixture {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        if (tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        versionOrder = List.copyOf(versionOrder); // null-checks the list and every element
        classes = List.copyOf(classes); // null-checks the list and every element
    }

    /**
     * Looks up a class in this scenario by its slug.
     *
     * @param slug the class slug to look up; never {@code null}
     * @return the matching {@link ClassFixture}
     * @throws IllegalArgumentException if no class in this scenario carries that slug
     */
    public ClassFixture classNamed(String slug) {
        return classes.stream()
                .filter(classFixture -> classFixture.slug().equals(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no class named '" + slug + "' in this fixture"));
    }
}
