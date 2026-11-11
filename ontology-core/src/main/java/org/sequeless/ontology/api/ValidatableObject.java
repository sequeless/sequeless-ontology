package org.sequeless.ontology.api;

import java.util.Map;
import java.util.Objects;
import org.sequeless.kernel.values.ObjectId;
import org.sequeless.kernel.values.Value;

/**
 * The object a {@link ShapeValidationPort} validates, expressed in kernel types. {@code id} is the
 * object's {@link ObjectId}. {@code typeIri} is the full IRI of its type. {@code propertiesByIri} maps
 * the full IRI of each property to the object's {@link Value} for it.
 *
 * <p>The properties map is copied on construction, so later changes to the map the caller passed in
 * do not change this record. The copy is made with {@link Map#copyOf(Map)}, which also rejects a
 * {@code null} key or value with {@link NullPointerException}.
 *
 * @param id the object's identity
 * @param typeIri the full IRI of the object's type
 * @param propertiesByIri the object's property values, keyed by the full property IRI
 */
public record ValidatableObject(ObjectId id, String typeIri, Map<String, Value> propertiesByIri) {

    /**
     * Requires every component to be non-{@code null} and stores an immutable copy of the properties.
     *
     * @throws NullPointerException if any component is {@code null}, or the properties map holds a
     *     {@code null} key or value
     */
    public ValidatableObject {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(typeIri, "typeIri must not be null");
        Objects.requireNonNull(propertiesByIri, "propertiesByIri must not be null");
        propertiesByIri = Map.copyOf(propertiesByIri);
    }
}
