package org.sequeless.ontology.testfixtures;

import org.sequeless.ontology.spi.OntologyDocumentStore;

public class InMemoryOntologyDocumentStoreContractTest extends OntologyDocumentStoreContract {

    @Override
    protected OntologyDocumentStore freshDocuments() {
        return new InMemoryOntologyDocumentStore();
    }
}
