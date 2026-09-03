package org.sequeless.ontology.facet.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FacetResolutionExceptionTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID CITY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

    private static LocalizedText label(String value) {
        return new LocalizedText(value, "en");
    }

    private static ClassRef classRef(String slug) {
        return new ClassRef(UUID.nameUUIDFromBytes(slug.getBytes()), slug, "urn:class:" + slug, label(slug));
    }

    private static FacetPath customerCity() {
        return new FacetPath(List.of(
                new PathSegment(CUSTOMER_ID, "customer", classRef("Customer")),
                new PathSegment(CITY_ID, "city", null)));
    }

    @Test
    void codeOnlyConstructorLeavesPathAndClassRefEmpty() {
        FacetResolutionException exception = new FacetResolutionException(FacetErrorCode.UNKNOWN_TENANT);

        assertThat(exception.code()).isEqualTo(FacetErrorCode.UNKNOWN_TENANT);
        assertThat(exception.path()).isEmpty();
        assertThat(exception.classRef()).isEmpty();
    }

    @Test
    void codeAndPathConstructorPopulatesPath() {
        FacetResolutionException exception = new FacetResolutionException(FacetErrorCode.UNKNOWN_PATH, customerCity());

        assertThat(exception.code()).isEqualTo(FacetErrorCode.UNKNOWN_PATH);
        assertThat(exception.path()).map(FacetPath::asSlugPath).contains("customer.city");
        assertThat(exception.classRef()).isEmpty();
    }

    @Test
    void codeAndClassRefConstructorPopulatesClassRef() {
        FacetResolutionException exception =
                new FacetResolutionException(FacetErrorCode.UNKNOWN_CLASS, classRef("Invoice"));

        assertThat(exception.code()).isEqualTo(FacetErrorCode.UNKNOWN_CLASS);
        assertThat(exception.path()).isEmpty();
        assertThat(exception.classRef()).map(ClassRef::slug).contains("Invoice");
    }

    @Test
    void allThreeConstructorPopulatesBoth() {
        FacetResolutionException exception =
                new FacetResolutionException(FacetErrorCode.CYCLE_DETECTED, customerCity(), classRef("Invoice"));

        assertThat(exception.code()).isEqualTo(FacetErrorCode.CYCLE_DETECTED);
        assertThat(exception.path()).isPresent();
        assertThat(exception.classRef()).isPresent();
    }

    @Test
    void messageIncludesCodeAndPath() {
        FacetResolutionException exception =
                new FacetResolutionException(FacetErrorCode.PATH_NOT_FILTERABLE, customerCity(), classRef("Invoice"));

        assertThat(exception.getMessage())
                .contains("PATH_NOT_FILTERABLE")
                .contains("customer.city")
                .contains("Invoice");
    }

    @Test
    void isAnUncheckedException() {
        assertThat(new FacetResolutionException(FacetErrorCode.DEPTH_EXCEEDED)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void conceptRejectsBlankIri() {
        assertThatThrownBy(() -> new Concept("  ", label("Laptops"), List.of(), Optional.empty(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iri");
    }

    @Test
    void conceptQueryAllowsNullPartialText() {
        ConceptQuery query = new ConceptQuery(null, Optional.of("urn:concept:Electronics"), Optional.empty(), 50);

        assertThat(query.partialText()).isNull();
        assertThat(query.parentIri()).contains("urn:concept:Electronics");
    }

    @Test
    void conceptDefensiveCopyIsolatesFromSourceMutation() {
        List<LocalizedText> altLabels = new ArrayList<>(List.of(label("Notebook")));
        Concept concept = new Concept("urn:concept:Laptops", label("Laptops"), altLabels, Optional.empty(), true);

        altLabels.add(label("Portable"));

        assertThat(concept.altLabels()).hasSize(1);
    }
}
