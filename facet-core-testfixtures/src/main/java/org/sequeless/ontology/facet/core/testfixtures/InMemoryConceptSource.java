package org.sequeless.ontology.facet.core.testfixtures;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.sequeless.ontology.facet.core.api.Concept;
import org.sequeless.ontology.facet.core.api.ConceptQuery;
import org.sequeless.ontology.facet.core.api.FacetErrorCode;
import org.sequeless.ontology.facet.core.api.FacetResolutionException;
import org.sequeless.ontology.facet.core.api.LocalizedText;
import org.sequeless.ontology.facet.core.spi.ConceptSource;

/**
 * An in-memory {@code ConceptSource} backed directly by a caller-supplied map of scheme id to its
 * concepts.
 *
 * <p>Unlike {@link InMemoryFacetSource}, this class deliberately does <em>not</em> build on {@link
 * ContractFixture}. Per {@link ConceptSource}'s own Javadoc (design.md section 4.7 and section
 * 3.2), a concept scheme is shared vocabulary published on its own clock, not tenant-owned schema —
 * it carries no tenant id and nothing in its data model relates it to a {@link ContractFixture}
 * scenario. Giving it its own minimal, self-contained storage keeps that independence honest rather
 * than borrowing a fixture shape built for a different port.
 */
public final class InMemoryConceptSource implements ConceptSource {

    private final Map<String, List<Concept>> conceptsByScheme;

    private InMemoryConceptSource(Map<String, List<Concept>> conceptsByScheme) {
        Map<String, List<Concept>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<Concept>> entry : conceptsByScheme.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.conceptsByScheme = Map.copyOf(copy);
    }

    /**
     * Wraps a map of scheme id to that scheme's concepts in a {@code ConceptSource}. Both the outer
     * map and every inner list are defensively copied.
     *
     * @param conceptsByScheme every scheme this source can serve, keyed by scheme id; never {@code
     *     null}
     * @return a source backed by {@code conceptsByScheme}
     */
    public static InMemoryConceptSource withSchemes(Map<String, List<Concept>> conceptsByScheme) {
        return new InMemoryConceptSource(conceptsByScheme);
    }

    @Override
    public List<Concept> concepts(String schemeId, ConceptQuery query) {
        List<Concept> scheme = requireScheme(schemeId);

        List<Concept> matches = new ArrayList<>();
        for (Concept concept : scheme) {
            if (!matchesPartialText(concept, query.partialText())) {
                continue;
            }
            if (query.parentIri().isPresent() && !query.parentIri().equals(concept.broaderIri())) {
                continue;
            }
            matches.add(concept);
            if (matches.size() == query.limit()) {
                break;
            }
        }
        return List.copyOf(matches);
    }

    @Override
    public Optional<Concept> concept(String schemeId, String conceptIri) {
        List<Concept> scheme = requireScheme(schemeId);
        return scheme.stream()
                .filter(concept -> concept.iri().equals(conceptIri))
                .findFirst();
    }

    private List<Concept> requireScheme(String schemeId) {
        List<Concept> scheme = conceptsByScheme.get(schemeId);
        if (scheme == null) {
            throw new FacetResolutionException(FacetErrorCode.SCHEME_UNAVAILABLE);
        }
        return scheme;
    }

    /**
     * Whether {@code concept} matches {@code partialText}: a case-insensitive substring match
     * against its preferred label or any of its alternative labels. A {@code null} {@code
     * partialText} means "no text filter" per {@link ConceptQuery#partialText()}'s Javadoc, and
     * matches everything.
     */
    private static boolean matchesPartialText(Concept concept, String partialText) {
        if (partialText == null) {
            return true;
        }
        String needle = partialText.toLowerCase(Locale.ROOT);
        if (concept.prefLabel().value().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        for (LocalizedText altLabel : concept.altLabels()) {
            if (altLabel.value().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
