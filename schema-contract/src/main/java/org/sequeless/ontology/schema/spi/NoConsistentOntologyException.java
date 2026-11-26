package org.sequeless.ontology.schema.spi;

/**
 * Thrown by a read port, such as {@link SchemaSource}, when no consistent Ontology is active. It is
 * unchecked, so intermediate callers need not declare it; the inbound boundary catches it once.
 *
 * <p>An adapter may throw a subtype that carries more detail, for example the report that explains
 * the inconsistency.
 */
public class NoConsistentOntologyException extends RuntimeException {

    /**
     * @param message the detail message
     */
    public NoConsistentOntologyException(String message) {
        super(message);
    }

    /**
     * @param message the detail message
     * @param cause the underlying cause
     */
    public NoConsistentOntologyException(String message, Throwable cause) {
        super(message, cause);
    }
}
