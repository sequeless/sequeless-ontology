package org.sequeless.ontology.jena;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sequeless.ontology.api.ValidationSchema;
import org.sequeless.ontology.schema.api.AttributeDefinition;
import org.sequeless.ontology.schema.api.Datatype;
import org.sequeless.ontology.schema.api.Schema;
import org.sequeless.ontology.schema.api.TypeDefinition;

/**
 * Presents a {@link Schema} as the {@link ValidationSchema} that a {@link
 * org.sequeless.ontology.api.ShapeValidationPort} reads: which
 * types exist, how they relate by supertype, and the XSD datatype of each attribute. It holds no state
 * beyond the schema it wraps.
 */
final class SchemaValidationSchema implements ValidationSchema {

    private final Schema schema;

    /**
     * @param schema the type system to expose; must not be {@code null}
     * @throws NullPointerException if {@code schema} is {@code null}
     */
    SchemaValidationSchema(Schema schema) {
        this.schema = Objects.requireNonNull(schema, "schema must not be null");
    }

    @Override
    public boolean hasType(String typeIri) {
        return schema.type(typeIri).isPresent();
    }

    /**
     * Walks {@link TypeDefinition#superTypes()} transitively, starting from {@code typeIri}. The walk
     * is needed because plain {@code ShaclValidator.validate(Shapes, Graph)} runs no reasoner over the
     * data graph: a {@code sh:targetClass} shape only fires against an {@code rdf:type} triple present
     * in the graph. A supertype that the schema cannot resolve stays in the closure but is not expanded, as in the prototype.
     *
     * @param typeIri a declared type IRI
     * @return {@code typeIri} and every supertype IRI reachable through {@code superTypes()} edges, as an unmodifiable set
     *     that starts with {@code typeIri}
     */
    @Override
    public Set<String> typeAndSupertypes(String typeIri) {
        Set<String> closure = new LinkedHashSet<>();
        closure.add(typeIri);
        Deque<String> pending = new ArrayDeque<>(List.of(typeIri));
        while (!pending.isEmpty()) {
            Optional<TypeDefinition> current = schema.type(pending.poll());
            if (current.isEmpty()) {
                continue;
            }
            for (String superType : current.get().superTypes()) {
                if (closure.add(superType)) {
                    pending.add(superType);
                }
            }
        }
        return Collections.unmodifiableSet(closure);
    }

    /**
     * @param typeIri a declared type IRI
     * @param propertyIri the property IRI
     * @return the XSD IRI of the {@link AttributeDefinition} that {@code typeIri} declares for
     *     {@code propertyIri}; the XSD string IRI when that property is not an attribute declared on the
     *     type, including when the type itself is not declared
     */
    @Override
    public String xsdDatatypeOf(String typeIri, String propertyIri) {
        return schema.type(typeIri)
                .flatMap(type -> type.properties().stream()
                        .filter(property -> property.iri().equals(propertyIri))
                        .reduce((first, second) -> second))
                .filter(AttributeDefinition.class::isInstance)
                .map(AttributeDefinition.class::cast)
                .map(attribute -> attribute.datatype().xsdIri())
                .orElse(Datatype.STRING.xsdIri());
    }
}
