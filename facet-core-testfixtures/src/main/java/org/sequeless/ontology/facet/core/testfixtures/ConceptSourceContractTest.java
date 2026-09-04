package org.sequeless.ontology.facet.core.testfixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.sequeless.ontology.facet.core.api.Concept;
import org.sequeless.ontology.facet.core.api.ConceptQuery;
import org.sequeless.ontology.facet.core.api.FacetErrorCode;
import org.sequeless.ontology.facet.core.api.FacetResolutionException;
import org.sequeless.ontology.facet.core.api.LocalizedText;
import org.sequeless.ontology.facet.core.spi.ConceptSource;

/**
 * The reusable contract test kit any {@link ConceptSource} implementation, including a future
 * RDF-backed one, must pass.
 *
 * <p>Every test method below exercises one guarantee documented on {@link ConceptSource}'s two
 * methods against one fixed, known scheme: {@link #SCHEME_ID}, whose concepts {@link
 * #categoriesConcepts()} builds. A concrete subclass supplies only {@link #sourceFor()} — a {@code
 * ConceptSource} that serves exactly that scheme, built from exactly {@link #categoriesConcepts()}
 * so the seed data behind the assertions here and the seed data behind the source under test never
 * drift apart — and inherits every test unchanged. See {@code docs/specs/facet-contract/design.md}
 * and {@code package-info}.
 */
public abstract class ConceptSourceContractTest {

    /** The scheme id every test method below queries; the id {@link #sourceFor()} must serve. */
    protected static final String SCHEME_ID = "categories";

    private static final String ELECTRONICS_IRI = "urn:concept:electronics";
    private static final String LAPTOPS_IRI = "urn:concept:laptops";
    private static final String ULTRABOOKS_IRI = "urn:concept:ultrabooks";
    private static final String FURNITURE_IRI = "urn:concept:furniture";

    /**
     * Builds the {@code ConceptSource} implementation under test, seeded with exactly the scheme
     * {@link #categoriesConcepts()} describes under {@link #SCHEME_ID}.
     *
     * @return a {@code ConceptSource} serving {@link #SCHEME_ID}
     */
    protected abstract ConceptSource sourceFor();

    // ------------------------------------------------------------------------------------------
    // Seed data -- shared verbatim between this class's assertions and every sourceFor()
    // implementation, so the two can never drift independently.
    // ------------------------------------------------------------------------------------------

    /**
     * The fixed, known two-level hierarchy every test method below queries:
     *
     * <ul>
     *   <li>{@code Electronics} ({@link #ELECTRONICS_IRI}) — top-level, has children
     *   <li>{@code Laptops} ({@link #LAPTOPS_IRI}) — child of Electronics, alt label "Notebook", has
     *       children
     *   <li>{@code Ultrabooks} ({@link #ULTRABOOKS_IRI}) — child of Laptops (not of Electronics), no
     *       children
     *   <li>{@code Furniture} ({@link #FURNITURE_IRI}) — a second top-level concept with no
     *       children, so a tree-picker test can distinguish "has children" from "does not"
     * </ul>
     *
     * @return the seed concepts for {@link #SCHEME_ID}
     */
    protected static List<Concept> categoriesConcepts() {
        return List.of(
                concept(ELECTRONICS_IRI, "Electronics", List.of(), Optional.empty(), true),
                concept(LAPTOPS_IRI, "Laptops", List.of("Notebook"), Optional.of(ELECTRONICS_IRI), true),
                concept(ULTRABOOKS_IRI, "Ultrabooks", List.of(), Optional.of(LAPTOPS_IRI), false),
                concept(FURNITURE_IRI, "Furniture", List.of(), Optional.empty(), false));
    }

    private static Concept concept(
            String iri, String prefLabel, List<String> altLabels, Optional<String> broaderIri, boolean hasNarrower) {
        return new Concept(
                iri,
                new LocalizedText(prefLabel, null),
                altLabels.stream().map(label -> new LocalizedText(label, null)).toList(),
                broaderIri,
                hasNarrower);
    }

    private static ConceptQuery query(String partialText, Optional<String> parentIri, int limit) {
        return new ConceptQuery(partialText, parentIri, Optional.empty(), limit);
    }

    private static FacetErrorCode codeOf(ThrowingCallable callable) {
        Throwable thrown = catchThrowable(callable);
        assertThat(thrown).isInstanceOf(FacetResolutionException.class);
        return ((FacetResolutionException) thrown).code();
    }

    // ------------------------------------------------------------------------------------------
    // concepts
    // ------------------------------------------------------------------------------------------

    @Test
    final void searchFiltersByPartialText() {
        ConceptSource source = sourceFor();

        List<Concept> byPrefLabel = source.concepts(SCHEME_ID, query("lap", Optional.empty(), 10));
        List<Concept> byAltLabel = source.concepts(SCHEME_ID, query("Notebook", Optional.empty(), 10));

        assertThat(byPrefLabel).extracting(Concept::iri).containsExactly(LAPTOPS_IRI);
        assertThat(byAltLabel).extracting(Concept::iri).containsExactly(LAPTOPS_IRI);
    }

    @Test
    final void treePickerListsDirectChildrenOfParent() {
        ConceptSource source = sourceFor();

        // Ultrabooks is a child of Laptops, not of Electronics -- only Laptops should come back.
        List<Concept> children = source.concepts(SCHEME_ID, query(null, Optional.of(ELECTRONICS_IRI), 10));

        assertThat(children).extracting(Concept::iri).containsExactly(LAPTOPS_IRI);
    }

    @Test
    final void limitIsRespected() {
        ConceptSource source = sourceFor();

        List<Concept> limited = source.concepts(SCHEME_ID, query(null, Optional.empty(), 2));

        assertThat(limited).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    final void unknownSchemeRaisesSchemeUnavailable() {
        // This is the test that completes 12/12 FacetErrorCode coverage across both contract kits:
        // FacetSourceContractTest#everyFacetErrorCodeIsReachable deliberately covers only the 11
        // codes FacetSource can raise (its own Javadoc excludes SCHEME_UNAVAILABLE as a
        // ConceptSource concern); this test pins that twelfth code.
        ConceptSource source = sourceFor();

        assertThat(codeOf(() -> source.concepts("no-such-scheme", query(null, Optional.empty(), 10))))
                .isEqualTo(FacetErrorCode.SCHEME_UNAVAILABLE);
        assertThat(codeOf(() -> source.concept("no-such-scheme", "any-iri")))
                .isEqualTo(FacetErrorCode.SCHEME_UNAVAILABLE);
    }

    // ------------------------------------------------------------------------------------------
    // concept
    // ------------------------------------------------------------------------------------------

    @Test
    final void hasNarrowerIsAccurateWithoutSecondRoundTrip() {
        ConceptSource source = sourceFor();

        Concept laptops = source.concept(SCHEME_ID, LAPTOPS_IRI).orElseThrow();
        Concept ultrabooks = source.concept(SCHEME_ID, ULTRABOOKS_IRI).orElseThrow();

        assertThat(laptops.hasNarrower()).isTrue();
        assertThat(ultrabooks.hasNarrower()).isFalse();
    }

    @Test
    final void conceptReturnsEmptyForUnknownIri() {
        ConceptSource source = sourceFor();

        Optional<Concept> result = source.concept(SCHEME_ID, "urn:concept:doesNotExist");

        assertThat(result).isEmpty();
    }
}
