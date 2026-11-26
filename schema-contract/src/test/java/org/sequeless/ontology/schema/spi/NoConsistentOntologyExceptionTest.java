package org.sequeless.ontology.schema.spi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NoConsistentOntologyExceptionTest {

    @Test
    void messageConstructorCarriesTheMessageAndNoCause() {
        NoConsistentOntologyException exception = new NoConsistentOntologyException("no active ontology");

        assertThat(exception.getMessage()).isEqualTo("no active ontology");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void messageAndCauseConstructorCarriesBoth() {
        IllegalStateException cause = new IllegalStateException("inconsistent");

        NoConsistentOntologyException exception = new NoConsistentOntologyException("no active ontology", cause);

        assertThat(exception.getMessage()).isEqualTo("no active ontology");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
