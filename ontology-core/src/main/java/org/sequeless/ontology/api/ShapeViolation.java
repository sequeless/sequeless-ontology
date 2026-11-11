package org.sequeless.ontology.api;

import java.util.Objects;

/**
 * One failed constraint found by a {@link ShapeValidationPort}. {@code propertyIri} is the full IRI of
 * the property the constraint concerns, or {@code ""} for a node-level violation that concerns the
 * object as a whole. {@code message} describes the failed constraint.
 *
 * @param propertyIri the full property IRI, or {@code ""} for a node-level violation
 * @param message a description of the failed constraint
 */
public record ShapeViolation(String propertyIri, String message) {

    /**
     * Requires both components to be non-{@code null}. An empty {@code propertyIri} is valid and marks a
     * node-level violation.
     *
     * @throws NullPointerException if {@code propertyIri} or {@code message} is {@code null}
     */
    public ShapeViolation {
        Objects.requireNonNull(propertyIri, "propertyIri must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
