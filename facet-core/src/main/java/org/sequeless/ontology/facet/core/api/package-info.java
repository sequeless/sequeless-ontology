/**
 * The published facet contract: the vocabulary {@code sequeless-ontology} and
 * {@code sequeless-filter} agree on for "what can be filtered". It exists as its own package in
 * its own zero-dependency module so that neither library appears on the other's dependency graph
 * — the ontology implements the ports, filter consumes the model, and nothing is inherited by
 * either side. See {@code docs/specs/facet-contract/design.md} section 2.
 *
 * <p>Everything here is an immutable record or a closed enum, with no behavior beyond derived
 * accessors, validation, and equality. All types here must be public.
 */
package org.sequeless.ontology.facet.core.api;
