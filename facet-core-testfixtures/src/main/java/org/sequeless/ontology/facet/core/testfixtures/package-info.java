/**
 * Implementation-neutral test support for the facet contract published by {@code facet-core}.
 *
 * <p>This package holds three things, deliberately kept together in one published test-support
 * artifact rather than split across modules:
 *
 * <ul>
 *   <li><b>Fixtures</b> ({@code ContractFixture}, {@code ClassFixture}, {@code PropertyFixture},
 *       {@code RelationshipFixture}, and the canned scenarios in {@code ContractFixtures}) — a
 *       small, ontology-shaped vocabulary for describing classes, properties, and relationships
 *       without committing to any one {@code FacetSource} implementation's internal model.
 *   <li><b>An in-memory reference implementation</b> of {@code FacetSource} and {@code
 *       ConceptSource} built directly from those fixtures, useful both as a lightweight stand-in
 *       for tests elsewhere in the reactor and as the executable specification against which the
 *       contract test kit below is itself proven correct.
 *   <li><b>A reusable contract test kit</b> — abstract JUnit test classes that any {@code
 *       FacetSource}/{@code ConceptSource} implementation, including an RDF-backed one added
 *       later, extends and points at its own instance to verify it upholds design.md's semantics
 *       (traversal guards, version policies, merge conflicts, and so on) rather than merely
 *       compiling against the contract's types.
 * </ul>
 *
 * <p>This module depends on {@code facet-core} and carries JUnit and AssertJ at <em>compile</em>
 * scope, not {@code test} scope, precisely because its main sources are themselves test
 * infrastructure meant to be reused by other modules' test code. It is never meant to reach a
 * consumer's compile classpath the way {@code facet-core} itself does; it is a test-support
 * artifact, published so downstream adapters can depend on it in their own {@code test} scope. See
 * {@code docs/specs/facet-contract/design.md} and ADR 0002.
 */
package org.sequeless.ontology.facet.core.testfixtures;
