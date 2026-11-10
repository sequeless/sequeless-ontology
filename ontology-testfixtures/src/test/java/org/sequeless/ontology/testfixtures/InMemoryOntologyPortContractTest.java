package org.sequeless.ontology.testfixtures;

import org.sequeless.ontology.api.OntologyDocument;
import org.sequeless.ontology.schema.api.Schema;

/**
 * This testkit's own proof that {@link OntologyContract} is not secretly biased toward
 * implementations that do real OWL reasoning. The port under test here does no OWL parsing at
 * all: it holds the current {@link OntologyDocument} verbatim, derives a fixed single-type
 * {@link Schema} and empty Behaviour and Automation halves from it regardless of content, and
 * decides consistency purely by comparing {@link OntologyDocument#content()} against {@link
 * Fixtures#inconsistentOntology()}'s content — genuine branching, trivially implemented, exactly
 * like {@code DenyAllAuthorizationPort} plays for {@code AuthorizationContract}. If {@link
 * OntologyContract} could only be passed by a port backed by a genuine reasoner, it would not be a
 * meaningful shared contract, and this test would fail.
 */
class InMemoryOntologyPortContractTest extends OntologyContract {

    @Override
    protected OntologyPorts portFor(OntologyDocument document) {
        return OntologyPorts.of(new InMemoryOntologyPort(document));
    }
}
