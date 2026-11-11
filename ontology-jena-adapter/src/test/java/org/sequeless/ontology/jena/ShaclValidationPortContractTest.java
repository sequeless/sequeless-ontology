package org.sequeless.ontology.jena;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.kernel.identity.TenantId;
import org.sequeless.kernel.values.ObjectId;
import org.sequeless.kernel.values.Value;
import org.sequeless.ontology.api.ShapeViolation;
import org.sequeless.ontology.api.ValidatableObject;
import org.sequeless.ontology.api.ValidationSchema;
import org.sequeless.ontology.testfixtures.Fixtures;

/**
 * Checks the {@link ShaclValidationPort} clauses of {@link org.sequeless.ontology.api.ShapeValidationPort}
 * that can be asserted directly: null handling, an unmodifiable result, and determinism for equal
 * arguments. The port runs against the reference ontology, whose {@code ex:TaskShape} bounds {@code
 * estimatedHours} to 0 through 1000 and whose {@code ex:PersonShape} pattern-checks {@code email}. The
 * invalid object below therefore produces a real violation, so the result is non-empty and the
 * unmodifiable check and the determinism check have something to compare. The valid-task and
 * violation-path cases check that each violation names the property IRI it concerns.
 */
class ShaclValidationPortContractTest {

    private static final Scope SCOPE = Fixtures.defaultScope();

    private static final JenaOntologyPort ONTOLOGY =
            JenaOntologyPort.fromDocument(Fixtures.referenceOntology(), ReasonerSetting.OWL, () -> Set.of("workload"));
    private static final ShaclValidationPort PORT = new ShaclValidationPort(ONTOLOGY);
    private static final ValidationSchema SCHEMA = new SchemaValidationSchema(ONTOLOGY.schema(TenantId.DEFAULT));

    private static final ValidatableObject INVALID_TASK =
            task(Map.of(Fixtures.ESTIMATED_HOURS_IRI, Value.decimal(new BigDecimal("-1"))));

    /** {@code ex:name} and {@code ex:email} on {@code ex:Person}; the testkit fixtures carry no constants for them. */
    private static final String NAME_IRI = "https://sequeless.dev/ns/ref#name";

    private static final String EMAIL_IRI = "https://sequeless.dev/ns/ref#email";

    @Test
    void rejectsNullScope() {
        assertThatNullPointerException().isThrownBy(() -> PORT.validate(null, SCHEMA, INVALID_TASK));
    }

    @Test
    void rejectsNullSchema() {
        assertThatNullPointerException().isThrownBy(() -> PORT.validate(SCOPE, null, INVALID_TASK));
    }

    @Test
    void rejectsNullObject() {
        assertThatNullPointerException().isThrownBy(() -> PORT.validate(SCOPE, SCHEMA, null));
    }

    @Test
    void resultIsNonNullAndUnmodifiableAndNamesTheViolatedProperty() {
        List<ShapeViolation> violations = PORT.validate(SCOPE, SCHEMA, INVALID_TASK);

        assertThat(violations).isNotNull();
        assertThat(violations)
                .isNotEmpty()
                .extracting(ShapeViolation::propertyIri)
                .containsOnly(Fixtures.ESTIMATED_HOURS_IRI);
        assertThatThrownBy(() -> violations.add(new ShapeViolation("", "extra")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void validReferenceTaskProducesNoViolations() {
        ValidatableObject person = person(Map.of(
                NAME_IRI, Value.text("Alice"),
                EMAIL_IRI, Value.text("alice@example.com")));
        ValidatableObject task = task(Map.of(
                Fixtures.TITLE_IRI, Value.text("Write plan"),
                Fixtures.ESTIMATED_HOURS_IRI, Value.decimal(new BigDecimal("40")),
                Fixtures.ASSIGNED_TO_IRI, Value.ref(person.id())));

        assertThat(PORT.validate(SCOPE, SCHEMA, person)).isEmpty();
        assertThat(PORT.validate(SCOPE, SCHEMA, task)).isEmpty();
    }

    @Test
    void outOfRangeEstimatedHoursProducesViolationOnItsPath() {
        ValidatableObject task = task(Map.of(
                Fixtures.TITLE_IRI, Value.text("Too big"),
                Fixtures.ESTIMATED_HOURS_IRI, Value.decimal(new BigDecimal("5000"))));

        List<ShapeViolation> violations = PORT.validate(SCOPE, SCHEMA, task);

        assertThat(violations)
                .isNotEmpty()
                .extracting(ShapeViolation::propertyIri)
                .containsOnly(Fixtures.ESTIMATED_HOURS_IRI);
    }

    @Test
    void invalidPersonEmailProducesViolationOnItsPath() {
        ValidatableObject person = person(Map.of(
                NAME_IRI, Value.text("Alice"),
                EMAIL_IRI, Value.text("not-an-email")));

        List<ShapeViolation> violations = PORT.validate(SCOPE, SCHEMA, person);

        assertThat(violations)
                .isNotEmpty()
                .extracting(ShapeViolation::propertyIri)
                .containsOnly(EMAIL_IRI);
    }

    @Test
    void repeatedCallsWithEqualArgumentsReturnEqualLists() {
        List<ShapeViolation> first = PORT.validate(SCOPE, SCHEMA, INVALID_TASK);
        List<ShapeViolation> second = PORT.validate(SCOPE, SCHEMA, INVALID_TASK);

        assertThat(second).isEqualTo(first);
    }

    private static ValidatableObject task(Map<String, Value> properties) {
        return new ValidatableObject(ObjectId.random(), Fixtures.TASK_IRI, properties);
    }

    private static ValidatableObject person(Map<String, Value> properties) {
        return new ValidatableObject(ObjectId.random(), Fixtures.PERSON_IRI, properties);
    }
}
