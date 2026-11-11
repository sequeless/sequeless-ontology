package org.sequeless.ontology.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class ShapeViolationTest {

    @Test
    void rejectsNullPropertyIri() {
        assertThatNullPointerException().isThrownBy(() -> new ShapeViolation(null, "must be present"));
    }

    @Test
    void rejectsNullMessage() {
        assertThatNullPointerException().isThrownBy(() -> new ShapeViolation("https://example.org/name", null));
    }

    @Test
    void acceptsEmptyPropertyIriForANodeLevelViolation() {
        ShapeViolation violation = new ShapeViolation("", "object is inconsistent");

        assertThat(violation.propertyIri()).isEmpty();
        assertThat(violation.message()).isEqualTo("object is inconsistent");
    }
}
