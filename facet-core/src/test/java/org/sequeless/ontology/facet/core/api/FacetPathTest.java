package org.sequeless.ontology.facet.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FacetPathTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID ADDRESS_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID CITY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

    private static ClassRef classRef(String slug) {
        return new ClassRef(UUID.nameUUIDFromBytes(slug.getBytes()), slug, "urn:class:" + slug, label(slug));
    }

    private static LocalizedText label(String value) {
        return new LocalizedText(value, "en");
    }

    @Test
    void classRefRejectsBlankSlug() {
        assertThatThrownBy(() -> new ClassRef(CUSTOMER_ID, "  ", "urn:class:Customer", label("Customer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slug");
    }

    @Test
    void classRefRejectsNullId() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new ClassRef(null, "Customer", "urn:class:Customer", label("Customer")));
    }

    @Test
    void pathSegmentAllowsNullTargetClassForTerminalSegment() {
        PathSegment terminal = new PathSegment(CITY_ID, "city", null);

        assertThat(terminal.targetClass()).isNull();
        assertThat(terminal.slug()).isEqualTo("city");
    }

    @Test
    void facetPathRejectsEmptySegments() {
        assertThatThrownBy(() -> new FacetPath(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void facetPathRejectsNonTerminalSegmentWithoutTargetClass() {
        // 'customer' is followed by 'city', so it must lead somewhere — but it has no targetClass.
        List<PathSegment> segments =
                List.of(new PathSegment(CUSTOMER_ID, "customer", null), new PathSegment(CITY_ID, "city", null));

        assertThatThrownBy(() -> new FacetPath(segments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetClass");
    }

    @Test
    void asSlugPathJoinsSegmentSlugsWithDots() {
        FacetPath path = new FacetPath(List.of(
                new PathSegment(CUSTOMER_ID, "customer", classRef("Customer")),
                new PathSegment(ADDRESS_ID, "address", classRef("Address")),
                new PathSegment(CITY_ID, "city", null)));

        assertThat(path.asSlugPath()).isEqualTo("customer.address.city");
    }

    @Test
    void asIdPathReturnsPropertyIdsInOrder() {
        FacetPath path = new FacetPath(List.of(
                new PathSegment(CUSTOMER_ID, "customer", classRef("Customer")),
                new PathSegment(CITY_ID, "city", null)));

        assertThat(path.asIdPath()).containsExactly(CUSTOMER_ID, CITY_ID);
    }

    @Test
    void asIdPathIsImmutable() {
        FacetPath path = new FacetPath(List.of(new PathSegment(CITY_ID, "city", null)));
        List<UUID> ids = path.asIdPath();

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> ids.add(CUSTOMER_ID));
    }

    @Test
    void isTraversingIsFalseForSingleSegmentPath() {
        FacetPath path = new FacetPath(List.of(new PathSegment(CITY_ID, "city", null)));

        assertThat(path.isTraversing()).isFalse();
    }

    @Test
    void isTraversingIsTrueForMultiSegmentPath() {
        FacetPath path = new FacetPath(List.of(
                new PathSegment(CUSTOMER_ID, "customer", classRef("Customer")),
                new PathSegment(CITY_ID, "city", null)));

        assertThat(path.isTraversing()).isTrue();
    }

    @Test
    void depthEqualsSegmentCount() {
        FacetPath path = new FacetPath(List.of(
                new PathSegment(CUSTOMER_ID, "customer", classRef("Customer")),
                new PathSegment(ADDRESS_ID, "address", classRef("Address")),
                new PathSegment(CITY_ID, "city", null)));

        assertThat(path.depth()).isEqualTo(3);
    }

    @Test
    void facetPathDefensiveCopyIsolatesFromSourceMutation() {
        // A mutable source is essential here: List.copyOf returns its argument unchanged when the
        // argument is already immutable, so passing List.of(...) would prove nothing.
        List<PathSegment> source = new ArrayList<>(List.of(new PathSegment(CITY_ID, "city", null)));
        FacetPath path = new FacetPath(source);

        source.add(new PathSegment(CUSTOMER_ID, "customer", classRef("Customer")));

        assertThat(path.segments()).hasSize(1);
        assertThat(path.asSlugPath()).isEqualTo("city");
    }

    @Test
    void facetTypeHasFourteenConstants() {
        assertThat(FacetType.values()).hasSize(14);
        assertThat(FacetType.values()).contains(FacetType.UNKNOWN);
    }

    @Test
    void facetErrorCodeHasTwelveConstants() {
        assertThat(FacetErrorCode.values()).hasSize(12);
    }
}
