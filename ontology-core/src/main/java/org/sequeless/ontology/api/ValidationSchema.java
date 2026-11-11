package org.sequeless.ontology.api;

import java.util.Set;

/**
 * The type information a {@link ShapeValidationPort} reads while it validates an object. It is the
 * schema the shapes are evaluated against, and it exposes only what validation needs: which types
 * exist, how they relate by supertype, and the datatype of each attribute.
 *
 * <p>Callers must check {@link #hasType(String)} before calling {@link #typeAndSupertypes(String)}
 * or {@link #xsdDatatypeOf(String, String)} for a type IRI.
 */
public interface ValidationSchema {

    /**
     * Reports whether the schema declares the type.
     *
     * @param typeIri the full IRI of the type
     * @return {@code true} if the schema declares {@code typeIri}, {@code false} otherwise
     */
    boolean hasType(String typeIri);

    /**
     * Returns the type and all of its supertypes. The set holds {@code typeIri} itself and every
     * transitive supertype of it. Call this only when {@link #hasType(String)} is {@code true} for
     * {@code typeIri}.
     *
     * @param typeIri the full IRI of a declared type
     * @return {@code typeIri} and every transitive supertype, including {@code typeIri} itself
     */
    Set<String> typeAndSupertypes(String typeIri);

    /**
     * Returns the XSD datatype IRI of an attribute property of a type. When {@code propertyIri} is
     * not an attribute of {@code typeIri}, or is not declared at all, the result is the XSD string
     * IRI, {@code http://www.w3.org/2001/XMLSchema#string}. Call this only when
     * {@link #hasType(String)} is {@code true} for {@code typeIri}.
     *
     * @param typeIri the full IRI of a declared type
     * @param propertyIri the full IRI of the property
     * @return the XSD datatype IRI of the attribute, or the XSD string IRI when the property is not an
     *     attribute of the type
     */
    String xsdDatatypeOf(String typeIri, String propertyIri);
}
