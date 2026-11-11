package org.sequeless.ontology.api;

import java.util.List;
import org.sequeless.kernel.identity.Scope;

/**
 * The inbound port that validates one {@link ValidatableObject} against the shapes of a
 * {@link ValidationSchema}. It is schema-free: the schema is passed in on each call, so the port
 * never holds a type system of its own, and the implementation is free to evaluate the shapes in
 * whatever way its ontology library provides.
 *
 * <p>Null handling: every argument, {@code scope}, {@code schema} and {@code object}, must be
 * non-{@code null}. A conforming implementation throws {@link NullPointerException} when any
 * argument is {@code null}.
 *
 * <p>The result is a non-{@code null}, unmodifiable list. An empty list means the object is valid. An implementation throws {@link IllegalArgumentException} when the type of {@code object} is not declared by {@code schema}.
 * Each {@link ShapeViolation} names the property IRI it concerns, or {@code ""} for a node-level
 * violation.
 */
public interface ShapeValidationPort {

    /**
     * Validates {@code object} against every shape that applies to its type in {@code schema}.
     *
     * @param scope the tenant and principal the validation runs for; a {@link Scope} from
     *     {@code kernel-identity}
     * @param schema the type information the shapes are read from
     * @param object the object to validate
     * @return the violations found, empty when the object is valid; never {@code null}
     */
    List<ShapeViolation> validate(Scope scope, ValidationSchema schema, ValidatableObject object);
}
