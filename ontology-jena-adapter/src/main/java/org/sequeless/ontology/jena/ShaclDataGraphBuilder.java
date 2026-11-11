package org.sequeless.ontology.jena;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.sequeless.kernel.values.BoolValue;
import org.sequeless.kernel.values.DateTimeValue;
import org.sequeless.kernel.values.DateValue;
import org.sequeless.kernel.values.DecimalValue;
import org.sequeless.kernel.values.IntegerValue;
import org.sequeless.kernel.values.ListValue;
import org.sequeless.kernel.values.ReferenceValue;
import org.sequeless.kernel.values.TextValue;
import org.sequeless.kernel.values.Value;
import org.sequeless.ontology.api.ValidatableObject;
import org.sequeless.ontology.api.ValidationSchema;

/**
 * Builds the single-object RDF data graph that {@link ShaclValidationPort} runs {@code
 * org.apache.jena.shacl.ShaclValidator} against: the object becomes {@code urn:uuid:<id>}, typed with
 * its own type IRI plus every ancestor type IRI from {@link ValidationSchema#typeAndSupertypes(String)}
 * (the full transitive closure, not just direct superclasses, because the validator runs no reasoner),
 * and each property either as a typed literal (a {@link TextValue}'s XSD type comes from {@link
 * ValidationSchema#xsdDatatypeOf(String, String)}) or as an {@code urn:uuid:<target>} object reference.
 * A {@link ListValue} expands into one triple per element rather than an RDF collection.
 *
 * <p>Package-private and stateless: a pure function of its two arguments, with no dependency on any
 * live Jena model, so {@code ShaclDataGraphBuilderTest} exercises it directly.
 */
final class ShaclDataGraphBuilder {

    private ShaclDataGraphBuilder() {}

    /**
     * Checks the object's type before anything else, so an unknown type is reported as such and never
     * reaches the supertype walk.
     *
     * @param schema the type system {@code object}'s type and properties are resolved against; must not
     *     be {@code null}
     * @param object the single object to render as an RDF data graph; must not be {@code null}
     * @return a freshly built {@link Graph} containing exactly {@code object}'s triples
     * @throws IllegalArgumentException if {@code object}'s type is not declared in {@code schema}, with
     *     the message {@code "Unknown type: <typeIri>"}
     */
    static Graph build(ValidationSchema schema, ValidatableObject object) {
        if (!schema.hasType(object.typeIri())) {
            throw new IllegalArgumentException("Unknown type: " + object.typeIri());
        }

        Model model = ModelFactory.createDefaultModel();
        Resource subject = model.createResource(subjectUri(object));

        for (String typeIri : schema.typeAndSupertypes(object.typeIri())) {
            model.add(subject, RDF.type, model.createResource(typeIri));
        }

        object.propertiesByIri()
                .forEach((propertyIri, value) -> addValue(
                        model, subject, propertyIri, value, schema.xsdDatatypeOf(object.typeIri(), propertyIri)));

        return model.getGraph();
    }

    private static String subjectUri(ValidatableObject object) {
        return "urn:uuid:" + object.id().value();
    }

    /**
     * @param textXsdIri the XSD datatype IRI a {@link TextValue} is typed with, as the schema declares
     *     it for {@code propertyIri}
     */
    private static void addValue(Model model, Resource subject, String propertyIri, Value value, String textXsdIri) {
        Property predicate = model.createProperty(propertyIri);
        switch (value) {
            case ListValue list -> list.values().forEach(v -> addValue(model, subject, propertyIri, v, textXsdIri));
            case ReferenceValue ref ->
                model.add(
                        subject,
                        predicate,
                        model.createResource("urn:uuid:" + ref.target().value()));
            case TextValue text -> model.add(subject, predicate, literal(model, text.value(), textXsdIri));
            case IntegerValue integer -> model.add(subject, predicate, model.createTypedLiteral(integer.value()));
            case DecimalValue decimal -> model.add(subject, predicate, model.createTypedLiteral(decimal.value()));
            case BoolValue bool -> model.add(subject, predicate, model.createTypedLiteral(bool.value()));
            case DateTimeValue dt ->
                model.add(
                        subject, predicate, model.createTypedLiteral(dt.value().toString(), XSDDatatype.XSDdateTime));
            case DateValue date ->
                model.add(
                        subject,
                        predicate,
                        model.createTypedLiteral(date.value().toString(), XSDDatatype.XSDdate));
        }
    }

    private static Literal literal(Model model, String lexical, String xsdIri) {
        return model.createTypedLiteral(lexical, TypeMapper.getInstance().getSafeTypeByName(xsdIri));
    }
}
