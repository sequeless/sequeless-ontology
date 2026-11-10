package org.sequeless.ontology.testfixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.ontology.api.OntologyDocument;
import org.sequeless.ontology.api.OntologyFormat;
import org.sequeless.ontology.spi.OntologyDocumentStore;
import org.sequeless.ontology.spi.StoredOntologyDocument;

/**
 * The mechanical form of the behavioural contract documented on {@link OntologyDocumentStore}'s
 * interface-level javadoc. Every {@link OntologyDocumentStore} implementation, adapter or test
 * double, is expected to satisfy every clause of that javadoc, and this class exercises each clause
 * once, against whatever store {@link #freshDocuments()} supplies.
 *
 * <p>To use this contract, extend it from a test class in your own module and implement {@link
 * #freshDocuments()} to return a brand-new, empty store under test:
 *
 * <pre>{@code
 * class MyDocumentStoreContractTest extends OntologyDocumentStoreContract {
 *     protected OntologyDocumentStore freshDocuments() {
 *         return new MyDocumentStore();
 *     }
 * }
 * }</pre>
 *
 * <p>This is the document-store part of the prototype's {@code ObjectStoreContract}. Only its two
 * document-store clauses are copied here. The prototype keeps both clauses until spec 12.
 */
public abstract class OntologyDocumentStoreContract {

    /**
     * @return the {@link OntologyDocumentStore} implementation under test, brand-new and empty;
     *     invoked fresh for every {@code @Test} method, so implementors may return a new instance
     *     each time or a shared one, whichever suits the store under test, but the returned store
     *     must not carry over state from a previous test
     */
    protected abstract OntologyDocumentStore freshDocuments();

    @Test
    void ontologyDocumentsRejectsNullArguments() {
        OntologyDocumentStore documents = freshDocuments();
        Scope scope = Fixtures.defaultScope();
        OntologyDocument document = new OntologyDocument("content", OntologyFormat.TURTLE);

        assertThatNullPointerException().isThrownBy(() -> documents.active(null));
        assertThatNullPointerException().isThrownBy(() -> documents.activate(null, document));
        assertThatNullPointerException().isThrownBy(() -> documents.activate(scope, null));
    }

    @Test
    void ontologyDocumentActiveIsEmptyInitiallyThenTracksVersions() {
        Scope scope = Fixtures.defaultScope();
        OntologyDocumentStore documents = freshDocuments();

        assertThat(documents.active(scope)).isEmpty();

        OntologyDocument first = new OntologyDocument("first content", OntologyFormat.TURTLE);
        StoredOntologyDocument stored1 = documents.activate(scope, first);
        assertThat(stored1.version()).isEqualTo(1);
        assertThat(documents.active(scope)).contains(stored1);

        OntologyDocument second = new OntologyDocument("second content", OntologyFormat.TURTLE);
        StoredOntologyDocument stored2 = documents.activate(scope, second);
        assertThat(stored2.version()).isEqualTo(2);

        Optional<StoredOntologyDocument> active = documents.active(scope);
        assertThat(active).isPresent();
        assertThat(active.orElseThrow().version()).isEqualTo(2);
        assertThat(active.orElseThrow().document()).isEqualTo(second);
    }
}
