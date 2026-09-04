package org.sequeless.ontology.facet.core.testfixtures;

import java.util.Map;
import org.sequeless.ontology.facet.core.spi.ConceptSource;

/**
 * Proves {@link InMemoryConceptSource} upholds every guarantee {@link ConceptSourceContractTest}
 * exercises, and serves as the executable specification the contract test kit itself is validated
 * against.
 */
class InMemoryConceptSourceContractTest extends ConceptSourceContractTest {

    @Override
    protected ConceptSource sourceFor() {
        return InMemoryConceptSource.withSchemes(Map.of(SCHEME_ID, categoriesConcepts()));
    }
}
