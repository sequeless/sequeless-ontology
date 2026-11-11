/**
 * The Apache Jena reference adapter of the ontology, backed by Jena ontapi.
 *
 * <p>The public types are {@code JenaOntologyPort}, which implements the schema, behaviour,
 * automation and administration ports, {@code ShaclValidationPort}, which implements the shape
 * validation port over the live ontology, {@code ReasonerSetting}, and {@code JenaOntologySettings}.
 *
 * <p>The compiler and the Jena internals are in {@code org.sequeless.ontology.jena.internal}. That
 * package is not part of the public API (ADR 0001).
 */
package org.sequeless.ontology.jena;
