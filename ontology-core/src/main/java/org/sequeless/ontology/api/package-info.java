/**
 * The ontology administration port: the outbound port that loads, reloads, exports and imports the
 * ontology, and the vocabulary of consistency reporting, documents, and formats it is expressed in
 * terms of.
 *
 * <p>Like every package in the ontology contracts, this package imports nothing outside the
 * JDK ({@code java.*}) except the kernel types it is expressed over: {@link
 * org.sequeless.kernel.identity.Scope} from kernel-identity, and {@link
 * org.sequeless.kernel.values.ObjectId} and {@link org.sequeless.kernel.values.Value} from
 * kernel-values. The validation api ({@link org.sequeless.ontology.api.ShapeValidationPort},
 * with {@link org.sequeless.ontology.api.ValidatableObject}, {@link
 * org.sequeless.ontology.api.ShapeViolation} and {@link org.sequeless.ontology.api.ValidationSchema})
 * is also in this package. Its implementation lives in ontology-jena-adapter.
 * The package does not depend on any half of the ontology in the schema, behaviour or automation contract. {@link
 * org.sequeless.ontology.api.OntologyAdministration} is the port interface; {@link
 * org.sequeless.ontology.api.OntologyDocument}, {@link org.sequeless.ontology.api.OntologyFormat},
 * {@link org.sequeless.ontology.api.ImportMode}, and {@link org.sequeless.ontology.api.ImportReport}
 * are its request and response vocabulary. {@link org.sequeless.ontology.api.OntologyReport} (with
 * its {@link org.sequeless.ontology.api.OntologyIssue} findings and {@link
 * org.sequeless.ontology.api.Severity} levels) is the consistency-reporting vocabulary shared by the
 * read ports, which live in the half packages, and by {@link
 * org.sequeless.ontology.api.OntologyException}, the unchecked exception those read ports and {@link
 * org.sequeless.ontology.api.OntologyAdministration#reload} throw when the ontology they would
 * otherwise return is inconsistent. The full behavioural contract every implementation must satisfy
 * is documented on {@link org.sequeless.ontology.api.OntologyAdministration} itself, and asserted
 * mechanically by {@code ontology-testfixtures}'s {@code OntologyContract}.
 */
package org.sequeless.ontology.api;
