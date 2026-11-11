package org.sequeless.ontology.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.values.ObjectId;
import org.sequeless.kernel.values.Value;

class ValidatableObjectTest {

    private static final ObjectId ID = ObjectId.random();

    private static final String TYPE_IRI = "https://example.org/Cyborg";

    @Test
    void rejectsNullId() {
        assertThatNullPointerException().isThrownBy(() -> new ValidatableObject(null, TYPE_IRI, Map.of()));
    }

    @Test
    void rejectsNullTypeIri() {
        assertThatNullPointerException().isThrownBy(() -> new ValidatableObject(ID, null, Map.of()));
    }

    @Test
    void rejectsNullPropertiesByIri() {
        assertThatNullPointerException().isThrownBy(() -> new ValidatableObject(ID, TYPE_IRI, null));
    }

    @Test
    void copiesThePropertiesMap() {
        Map<String, Value> properties = new HashMap<>();
        properties.put("https://example.org/name", Value.text("Marvin"));
        ValidatableObject object = new ValidatableObject(ID, TYPE_IRI, properties);

        properties.put("https://example.org/serial", Value.integer(42));

        assertThat(object.propertiesByIri()).containsOnlyKeys("https://example.org/name");
    }
}
