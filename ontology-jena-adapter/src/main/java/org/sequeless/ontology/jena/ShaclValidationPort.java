package org.sequeless.ontology.jena;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.Path;
import org.sequeless.kernel.identity.Scope;
import org.sequeless.ontology.api.ShapeValidationPort;
import org.sequeless.ontology.api.ShapeViolation;
import org.sequeless.ontology.api.ValidatableObject;
import org.sequeless.ontology.api.ValidationSchema;

/**
 * The default {@link ShapeValidationPort} adapter, backed by {@code org.apache.jena.shacl} (jena-shacl,
 * verified against the resolved {@code jena-shacl-6.1.0.jar}).
 *
 * <h2>Why this class depends on the concrete {@link JenaOntologyPort}, not an interface</h2>
 *
 * <p>SHACL shapes live in the same ontology document the running {@link JenaOntologyPort} serves, and
 * must always reflect whatever that port's current ontology is, including after a live {@code
 * importDocument} swaps it. {@link JenaOntologyPort#currentModel(Scope)} is the package-private accessor
 * that exposes the live {@code OntModel}, and through {@link org.apache.jena.rdf.model.Model#getGraph()}
 * the shapes graph embedded in it, to code in this package. The ontology api packages expose no Jena
 * type, so this class takes the concrete port in its constructor instead.
 */
public final class ShaclValidationPort implements ShapeValidationPort {

    private final JenaOntologyPort ontologyPort;

    /**
     * @param ontologyPort the Jena adapter whose live ontology this port reads {@code sh:} shapes from;
     *     must not be {@code null}
     * @throws NullPointerException if {@code ontologyPort} is {@code null}
     */
    public ShaclValidationPort(JenaOntologyPort ontologyPort) {
        this.ontologyPort = Objects.requireNonNull(ontologyPort, "ontologyPort must not be null");
    }

    /**
     * Validates {@code object} against the shapes of the live ontology. The shapes are read for {@code
     * scope} at call time, and the object's data graph is built from {@code schema} and {@code object}
     * alone, so the same arguments always produce the same violations.
     *
     * @param scope the tenant and principal the validation runs for; must not be {@code null}
     * @param schema the type information the data graph is built against; must not be {@code null}
     * @param object the object to validate; must not be {@code null}
     * @return the violations found, empty when the object is valid; an unmodifiable list, never {@code
     *     null}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code object}'s type is not declared in {@code schema}
     */
    @Override
    public List<ShapeViolation> validate(Scope scope, ValidationSchema schema, ValidatableObject object) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(object, "object must not be null");

        Graph shapesGraph = ontologyPort.currentModel(scope).getGraph();
        Shapes shapes = Shapes.parse(shapesGraph);
        Graph dataGraph = ShaclDataGraphBuilder.build(schema, object);

        ValidationReport report = ShaclValidator.get().validate(shapes, dataGraph);

        List<ShapeViolation> violations = new ArrayList<>();
        for (ReportEntry entry : report.getEntries()) {
            violations.add(new ShapeViolation(pathIriOf(entry), messageOf(entry)));
        }
        return List.copyOf(violations);
    }

    /**
     * @param entry a single non-conforming {@link ReportEntry}
     * @return the IRI of {@link ReportEntry#resultPath()}'s single {@code sh:path} predicate, or {@code
     *     ""} when the entry has no path (a node-level constraint). Every {@code sh:path} in this
     *     project's shapes is a plain predicate IRI, which jena-shacl represents as a {@link P_Link}, so
     *     that is the only case unwrapped here. A compound path (sequence, inverse, alternative) is not
     *     produced by any shape this project declares, so it falls back to {@link Path#toString()}.
     */
    private static String pathIriOf(ReportEntry entry) {
        Path path = entry.resultPath();
        if (path == null) {
            return "";
        }
        if (path instanceof P_Link link) {
            return link.getNode().getURI();
        }
        return path.toString();
    }

    private static String messageOf(ReportEntry entry) {
        String message = entry.message();
        return (message == null || message.isBlank()) ? "SHACL constraint violated" : message;
    }
}
