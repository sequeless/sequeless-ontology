package org.sequeless.ontology.jena.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;
import org.sequeless.filter.api.AndFilter;
import org.sequeless.filter.api.FieldDefinition;
import org.sequeless.filter.api.FieldFilter;
import org.sequeless.filter.api.FieldRegistry;
import org.sequeless.filter.api.FieldRegistrySpec;
import org.sequeless.filter.api.FilterNode;
import org.sequeless.filter.api.FilterValidator;
import org.sequeless.filter.api.OperatorRegistry;
import org.sequeless.ontology.api.Severity;
import org.sequeless.ontology.behaviour.api.RollupRule;
import org.sequeless.ontology.jena.ReasonerSetting;

/**
 * Checks the {@code sq:filter} to filter-core mapping (spec 11, section 3.2) on inline Turtle: one
 * case per {@code sq:operator} with its canonical filter operator and JSON value, the zero, one and
 * two clause shapes, and the full property IRI as the path (Q5). Every mapped node must pass
 * filter-core's {@link FilterValidator} against a registry built for this fixture.
 */
class RollupRuleMappingTest {

    private static final String NS = "https://sequeless.dev/ns/ref#";
    private static final String OPEN_TASK_COUNT_IRI = NS + "openTaskCount";
    private static final String STATUS_IRI = NS + "status";
    private static final String PRIORITY_IRI = NS + "priority";
    private static final String BELONGS_TO_PROJECT_IRI = NS + "belongsToProject";
    private static final String PROJECT_UUID = "7d1e2f3a-4b5c-4d6e-8f70-123456789abc";

    private static final String HEADER = """
        @prefix sq:   <https://sequeless.dev/ns/meta#> .
        @prefix owl:  <http://www.w3.org/2002/07/owl#> .
        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .
        @prefix ex:   <https://sequeless.dev/ns/ref#> .

        <https://sequeless.dev/ns/ref> a owl:Ontology ; owl:imports <https://sequeless.dev/ns/meta> .

        ex:Project a owl:Class .
        ex:Task a owl:Class .

        ex:status a owl:DatatypeProperty ; rdfs:domain ex:Task ; rdfs:range xsd:string .
        ex:priority a owl:DatatypeProperty ; rdfs:domain ex:Task ; rdfs:range xsd:integer .
        ex:belongsToProject a owl:ObjectProperty ; rdfs:domain ex:Task ; rdfs:range ex:Project .

        """;

    /** The fields the clauses name, typed as the query side types them (JSON Schema). */
    private static final FieldRegistry FIELDS = FieldRegistry.of(FieldRegistrySpec.builder()
            .fields(List.of(
                    FieldDefinition.builder()
                            .path(STATUS_IRI)
                            .jsonSchemaType("string")
                            .build(),
                    FieldDefinition.builder()
                            .path(PRIORITY_IRI)
                            .jsonSchemaType("integer")
                            .build(),
                    FieldDefinition.builder()
                            .path(BELONGS_TO_PROJECT_IRI)
                            .jsonSchemaType("string")
                            .build()))
            .build());

    /**
     * One {@code sq:operator} case: the Turtle term of the property and of the operator, the
     * expected path, the Turtle literal of {@code sq:value} (null when the operator takes none), and
     * the expected filter operator and JSON value.
     */
    private record OperatorCase(
            String sqOperator, String propertyTerm, String propertyIri, String value, String filterOp, JsonNode json) {}

    @Test
    void everyOperatorMapsToItsCanonicalFilterOperatorAndPassesTheValidator() {
        List<OperatorCase> cases = List.of(
                new OperatorCase("eq", "ex:status", STATUS_IRI, "\"done\"", "is", TextNode.valueOf("done")),
                new OperatorCase("ne", "ex:status", STATUS_IRI, "\"done\"", "is not", TextNode.valueOf("done")),
                new OperatorCase(
                        "in",
                        "ex:status",
                        STATUS_IRI,
                        "\"done\"",
                        "is in",
                        JsonNodeFactory.instance.arrayNode().add(TextNode.valueOf("done"))),
                new OperatorCase("lt", "ex:priority", PRIORITY_IRI, "\"3\"", "is less than", LongNode.valueOf(3)),
                new OperatorCase(
                        "lte", "ex:priority", PRIORITY_IRI, "\"3\"", "is less than or equal to", LongNode.valueOf(3)),
                new OperatorCase("gt", "ex:priority", PRIORITY_IRI, "\"3\"", "is greater than", LongNode.valueOf(3)),
                new OperatorCase(
                        "gte",
                        "ex:priority",
                        PRIORITY_IRI,
                        "\"3\"",
                        "is greater than or equal to",
                        LongNode.valueOf(3)),
                new OperatorCase("contains", "ex:status", STATUS_IRI, "\"do\"", "contains", TextNode.valueOf("do")),
                new OperatorCase("startsWith", "ex:status", STATUS_IRI, "\"d\"", "starts with", TextNode.valueOf("d")),
                new OperatorCase("isNull", "ex:status", STATUS_IRI, null, "is", NullNode.instance),
                new OperatorCase("notNull", "ex:status", STATUS_IRI, null, "is not", NullNode.instance));

        for (OperatorCase c : cases) {
            FilterNode criteria = openTaskCountRule(filter(clause(c.propertyTerm(), c.sqOperator(), c.value())))
                    .criteria()
                    .orElseThrow();
            assertThat(criteria).as(c.sqOperator()).isEqualTo(new FieldFilter(c.propertyIri(), c.filterOp(), c.json()));
            assertValid(criteria, c.sqOperator());
        }
    }

    @Test
    void noClauseMapsToEmptyCriteria() {
        assertThat(openTaskCountRule("").criteria()).isEmpty();
    }

    @Test
    void oneClauseMapsToItsFieldFilter() {
        FilterNode criteria = openTaskCountRule(filter(clause("ex:status", "ne", "\"done\"")))
                .criteria()
                .orElseThrow();

        assertThat(criteria).isEqualTo(new FieldFilter(STATUS_IRI, "is not", TextNode.valueOf("done")));
        assertValid(criteria, "one clause");
    }

    @Test
    void twoClausesMapToAnAndFilterInTheirOrder() {
        FilterNode criteria = openTaskCountRule(
                        filter(clause("ex:status", "ne", "\"done\""), clause("ex:priority", "gt", "\"3\"")))
                .criteria()
                .orElseThrow();

        assertThat(criteria)
                .isEqualTo(new AndFilter(List.of(
                        new FieldFilter(STATUS_IRI, "is not", TextNode.valueOf("done")),
                        new FieldFilter(PRIORITY_IRI, "is greater than", LongNode.valueOf(3)))));
        assertValid(criteria, "two clauses");
    }

    @Test
    void objectReferenceValueIsItsCanonicalUuidString() {
        FilterNode criteria = openTaskCountRule(filter(clause("ex:belongsToProject", "eq", "\"" + PROJECT_UUID + "\"")))
                .criteria()
                .orElseThrow();

        assertThat(criteria).isEqualTo(new FieldFilter(BELONGS_TO_PROJECT_IRI, "is", TextNode.valueOf(PROJECT_UUID)));
        assertValid(criteria, "reference");
    }

    private static void assertValid(FilterNode node, String label) {
        assertThat(FilterValidator.validate(node, OperatorRegistry.defaults(), FIELDS))
                .as(label)
                .isEmpty();
    }

    /** One {@code sq:Criterion} node; {@code value} is a Turtle literal, or null for an operator that takes none. */
    private static String clause(String property, String sqOperator, String value) {
        String valueTerm = value == null ? "" : " ; sq:value " + value;
        return "[ a sq:Criterion ; sq:property " + property + " ; sq:operator sq:" + sqOperator + valueTerm + " ]";
    }

    /** An {@code sq:filter} predicate over the given clauses, in order. */
    private static String filter(String... clauses) {
        return "sq:filter ( " + String.join(" ", clauses) + " ) ;";
    }

    /** Maps a {@code sq:Rollup} over {@code ex:Task} with {@code filterTerm} (possibly empty). */
    private static RollupRule openTaskCountRule(String filterTerm) {
        MappingResult result = SnapshotMapper.map(buildModel(HEADER + """
                ex:openTaskCount
                    a owl:DatatypeProperty ;
                    rdfs:domain ex:Project ;
                    rdfs:range xsd:integer ;
                    sq:derivedBy [
                        a sq:Rollup ;
                        sq:function sq:count ;
                        sq:over ex:Task ;
                        sq:via ex:belongsToProject ;
                        %s ] .
                """.formatted(filterTerm)));

        assertThat(result.issues()).noneMatch(issue -> issue.severity() == Severity.ERROR);
        return (RollupRule) result.derivations().get(OPEN_TASK_COUNT_IRI);
    }

    private static OntModel buildModel(String turtle) {
        Model rdfModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(
                rdfModel,
                new ByteArrayInputStream(turtle.getBytes(StandardCharsets.UTF_8)),
                "https://sequeless.dev/ns/ref",
                Lang.TURTLE);
        return OntModelFactory.createModel(
                rdfModel.getGraph(), ReasonerSetting.NONE.specification(), ImportResolver.withBundledMeta());
    }
}
