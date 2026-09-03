package org.sequeless.ontology.facet.core.testfixtures;

import org.sequeless.ontology.facet.core.spi.FacetSource;

/**
 * Proves {@link InMemoryFacetSource} upholds every guarantee {@link FacetSourceContractTest}
 * exercises, and serves as the executable specification the contract test kit itself is validated
 * against.
 */
class InMemoryFacetSourceContractTest extends FacetSourceContractTest {

    @Override
    protected FacetSource sourceFor(ContractFixture fixture) {
        return InMemoryFacetSource.from(fixture);
    }
}
