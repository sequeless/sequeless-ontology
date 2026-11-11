package org.sequeless.ontology.jena;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.identity.TenantId;
import org.sequeless.kernel.values.ObjectId;
import org.sequeless.kernel.values.Value;
import org.sequeless.ontology.api.ValidatableObject;
import org.sequeless.ontology.api.ValidationSchema;
import org.sequeless.ontology.testfixtures.Fixtures;

/**
 * Plain unit tests for {@link ShaclDataGraphBuilder}: no Spring and no {@link ShaclValidationPort}. They
 * exercise the RDF shape of the graph it builds directly, against a real {@link ValidationSchema} built
 * from the reference ontology (needed for a real supertype chain and real attribute datatypes).
 */
class ShaclDataGraphBuilderTest {

    private static final ValidationSchema SCHEMA = new SchemaValidationSchema(
            JenaOntologyPort.fromDocument(Fixtures.referenceOntology(), ReasonerSetting.OWL, () -> Set.of("workload"))
                    .schema(TenantId.DEFAULT));

    @Test
    void referenceValueProducesResourceObjectNotLiteral() {
        ObjectId personId = ObjectId.random();
        ValidatableObject task = task(Map.of(Fixtures.ASSIGNED_TO_IRI, Value.ref(personId)));

        Graph graph = ShaclDataGraphBuilder.build(SCHEMA, task);

        Node subject = NodeFactory.createURI("urn:uuid:" + task.id().value());
        Node predicate = NodeFactory.createURI(Fixtures.ASSIGNED_TO_IRI);
        Node expectedObject = NodeFactory.createURI("urn:uuid:" + personId.value());

        List<Node> objects = graph.find(subject, predicate, Node.ANY)
                .mapWith(triple -> triple.getObject())
                .toList();
        assertThat(objects).containsExactly(expectedObject);
        assertThat(objects.get(0).isURI()).isTrue();
    }

    @Test
    void rdfTypeTriplesIncludeFullTransitiveSupertypeClosure() {
        ValidatableObject task = task(Map.of());

        Graph graph = ShaclDataGraphBuilder.build(SCHEMA, task);

        Node subject = NodeFactory.createURI("urn:uuid:" + task.id().value());
        List<Node> types = graph.find(subject, RDF.type.asNode(), Node.ANY)
                .mapWith(triple -> triple.getObject())
                .toList();

        assertThat(types)
                .extracting(Node::getURI)
                .containsExactlyInAnyOrder(Fixtures.TASK_IRI, Fixtures.WORK_ITEM_IRI, Fixtures.DELIVERABLE_IRI);
    }

    @Test
    void estimatedHoursProducesScalePreservingXsdDecimalLiteral() {
        BigDecimal exact = new BigDecimal("40.50");
        ValidatableObject task = task(Map.of(Fixtures.ESTIMATED_HOURS_IRI, Value.decimal(exact)));

        Graph graph = ShaclDataGraphBuilder.build(SCHEMA, task);
        Model model = ModelFactory.createModelForGraph(graph);

        Node subject = NodeFactory.createURI("urn:uuid:" + task.id().value());
        Node predicate = NodeFactory.createURI(Fixtures.ESTIMATED_HOURS_IRI);
        Node literal = graph.find(subject, predicate, Node.ANY).next().getObject();

        assertThat(literal.isLiteral()).isTrue();
        assertThat(literal.getLiteralDatatypeURI()).isEqualTo("http://www.w3.org/2001/XMLSchema#decimal");
        assertThat(literal.getLiteralLexicalForm()).isEqualTo(exact.toString());
        model.close();
    }

    @Test
    void multiValuedListProducesOneTriplePerElementNotAnRdfCollection() {
        ValidatableObject task =
                task(Map.of(Fixtures.STATUS_IRI, Value.list(List.of(Value.text("OPEN"), Value.text("URGENT")))));

        Graph graph = ShaclDataGraphBuilder.build(SCHEMA, task);

        Node subject = NodeFactory.createURI("urn:uuid:" + task.id().value());
        Node predicate = NodeFactory.createURI(Fixtures.STATUS_IRI);
        List<Node> values = graph.find(subject, predicate, Node.ANY)
                .mapWith(triple -> triple.getObject())
                .toList();

        assertThat(values).hasSize(2);
        assertThat(values).extracting(Node::getLiteralLexicalForm).containsExactlyInAnyOrder("OPEN", "URGENT");
    }

    @Test
    void unknownTypeIsRejectedWithItsIri() {
        String unknownType = "https://sequeless.dev/ns/ref#Nonexistent";
        ValidatableObject unknown = new ValidatableObject(ObjectId.random(), unknownType, Map.of());

        assertThatThrownBy(() -> ShaclDataGraphBuilder.build(SCHEMA, unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown type: " + unknownType);
    }

    private static ValidatableObject task(Map<String, Value> properties) {
        return new ValidatableObject(ObjectId.random(), Fixtures.TASK_IRI, properties);
    }
}
