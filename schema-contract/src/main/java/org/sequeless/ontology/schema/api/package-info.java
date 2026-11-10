/**
 * The Schema half of an ontology activation: immutable value types describing the application's
 * type system as read from OWL, independent of any RDF library.
 *
 * <p>Like every package in the ontology contracts, this package imports nothing outside the
 * JDK ({@code java.*}) and the kernel ({@code org.sequeless.kernel.*}). {@link
 * org.sequeless.ontology.schema.api.Schema} is the aggregate root: the ontology IRI, its declared
 * prefixes, the {@link org.sequeless.ontology.schema.api.OntologyVersion} of the activation it was read
 * from, and every {@link org.sequeless.ontology.schema.api.TypeDefinition} it contains. A type carries
 * {@link org.sequeless.ontology.schema.api.PropertyDefinition}s, sealed to {@link
 * org.sequeless.ontology.schema.api.AttributeDefinition} (scalar-valued) and {@link
 * org.sequeless.ontology.schema.api.RelationshipDefinition} (object-valued). {@link
 * org.sequeless.ontology.schema.api.Cardinality} and {@link org.sequeless.ontology.schema.api.DisplayHints}
 * are shared value types. {@link org.sequeless.ontology.schema.api.AggregateFunction} is the shared
 * function vocabulary of {@code AggregateRequest}. {@link
 * org.sequeless.ontology.schema.spi.SchemaSource} is the read port for this half.
 *
 * <p>The state machines and derivation rules that govern these types are in {@code
 * org.sequeless.ontology.behaviour.api}, and the actions that run when a transition fires are in {@code
 * org.sequeless.ontology.automation.api}. The full mapping from OWL and the {@code sq:} annotation
 * vocabulary to these fields is documented in {@code docs/architecture/sq-vocabulary.md}, which this
 * package's field names must match exactly.
 */
package org.sequeless.ontology.schema.api;
