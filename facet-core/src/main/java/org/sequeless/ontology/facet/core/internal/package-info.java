/**
 * Implementation detail of {@code facet-core}: pure functions over the model — version merging,
 * scope stamping, and facet path encoding. These live in the contract rather than in each
 * implementation so that every {@code FacetSource} gets identical semantics for free, instead of
 * each one reinventing — and getting subtly different — answers to "what happens when v1 and v2
 * disagree".
 *
 * <p>This package is <strong>not</strong> published surface. Nothing outside this module may
 * depend on it, and {@code api} and {@code spi} never depend on it; both rules are enforced by
 * ArchUnit.
 */
package org.sequeless.ontology.facet.core.internal;
