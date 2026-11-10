package org.sequeless.ontology.jena.internal;

import java.io.IOException;
import java.io.InputStream;
import org.apache.jena.graph.Graph;
import org.apache.jena.ontapi.GraphRepository;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 * Pre-registers this adapter's own bundled copy of the {@code sq:} vocabulary against its ontology
 * IRI in a fresh {@link GraphRepository}, so that {@code owl:imports <https://sequeless.dev/ns/meta>}
 * — which every ontology using the {@code sq:} vocabulary declares — resolves without ever touching
 * the network. Any other {@code owl:imports} target is left unregistered and fails inside {@code
 * OntModelFactory.createModel}; {@link OntModelBuilder} is responsible for catching that and
 * turning it into an {@code ERROR} {@code OntologyIssue} rather than letting a raw Jena exception
 * escape the adapter.
 *
 * <p>The vocabulary is bundled as two files, {@code sq-schema.ttl} and {@code sq-behaviour.ttl},
 * which are merged into one graph under {@link #SQ_META_IRI}. The adapter ships its own copy,
 * because it cannot depend on a testkit outside test scope.
 */
public final class ImportResolver {

    public static final String SQ_META_IRI = "https://sequeless.dev/ns/meta";

    private static final String SQ_META_SCHEMA_RESOURCE = "ontology/sq-schema.ttl";

    private static final String SQ_META_BEHAVIOUR_RESOURCE = "ontology/sq-behaviour.ttl";

    private ImportResolver() {}

    /**
     * @return a new, in-memory {@link GraphRepository} pre-populated with one graph under {@link
     *     #SQ_META_IRI}, the bundled {@code sq-schema.ttl} and {@code sq-behaviour.ttl} merged
     */
    static GraphRepository withBundledMeta() {
        GraphRepository repository = GraphRepository.createGraphDocumentRepositoryMem();
        repository.put(SQ_META_IRI, loadBundledMetaGraph());
        return repository;
    }

    private static Graph loadBundledMetaGraph() {
        Model model = ModelFactory.createDefaultModel();
        readBundled(model, SQ_META_SCHEMA_RESOURCE);
        readBundled(model, SQ_META_BEHAVIOUR_RESOURCE);
        return model.getGraph();
    }

    private static void readBundled(Model model, String classpathResource) {
        try (InputStream in = ImportResolver.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("bundled resource missing from classpath: " + classpathResource);
            }
            RDFDataMgr.read(model, in, SQ_META_IRI, Lang.TURTLE);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read bundled resource: " + classpathResource, e);
        }
    }
}
