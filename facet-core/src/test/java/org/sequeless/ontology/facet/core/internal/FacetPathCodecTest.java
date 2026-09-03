package org.sequeless.ontology.facet.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.sequeless.ontology.facet.core.api.ClassRef;
import org.sequeless.ontology.facet.core.api.FacetPath;
import org.sequeless.ontology.facet.core.api.LocalizedText;
import org.sequeless.ontology.facet.core.api.PathSegment;

class FacetPathCodecTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID ADDRESS_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID CITY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

    private static ClassRef classRef(String slug) {
        return new ClassRef(UUID.nameUUIDFromBytes(slug.getBytes()), slug, "urn:class:" + slug, label(slug));
    }

    private static LocalizedText label(String value) {
        return new LocalizedText(value, "en");
    }

    private static FacetPath traversingPath() {
        return new FacetPath(List.of(
                new PathSegment(CUSTOMER_ID, "customer", classRef("Customer")),
                new PathSegment(ADDRESS_ID, "address", classRef("Address")),
                new PathSegment(CITY_ID, "city", null)));
    }

    @Test
    void formatIdsJoinsPropertyIdsWithSlash() {
        assertThat(FacetPathCodec.formatIds(traversingPath()))
                .isEqualTo(CUSTOMER_ID + "/" + ADDRESS_ID + "/" + CITY_ID);
    }

    @Test
    void parseIdsSplitsOnSlashAndParsesEachUuid() {
        String idPath = CUSTOMER_ID + "/" + ADDRESS_ID + "/" + CITY_ID;

        assertThat(FacetPathCodec.parseIds(idPath)).containsExactly(CUSTOMER_ID, ADDRESS_ID, CITY_ID);
    }

    @Test
    void formatIdsAndParseIdsRoundTripPreservesIdentity() {
        FacetPath path = traversingPath();

        List<UUID> roundTripped = FacetPathCodec.parseIds(FacetPathCodec.formatIds(path));

        assertThat(roundTripped).isEqualTo(path.asIdPath());
    }

    @Test
    void parseIdsRejectsBlankInput() {
        assertThatThrownBy(() -> FacetPathCodec.parseIds("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void parseIdsRejectsBlankSegment() {
        assertThatThrownBy(() -> FacetPathCodec.parseIds(CUSTOMER_ID + "//" + CITY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank segment");
    }

    @Test
    void parseIdsRejectsMalformedUuidSegment() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> FacetPathCodec.parseIds("not-a-uuid"));
    }

    @Test
    void formatSlugDelegatesToAsSlugPath() {
        FacetPath path = traversingPath();

        assertThat(FacetPathCodec.formatSlug(path)).isEqualTo(path.asSlugPath());
    }

    @Test
    void splitSlugSplitsOnDots() {
        assertThat(FacetPathCodec.splitSlug("customer.address.city")).containsExactly("customer", "address", "city");
    }

    @Test
    void splitSlugRejectsBlankInput() {
        assertThatThrownBy(() -> FacetPathCodec.splitSlug("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void splitSlugRejectsLeadingDot() {
        assertThatThrownBy(() -> FacetPathCodec.splitSlug(".customer.city"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank segment");
    }

    @Test
    void splitSlugRejectsTrailingDot() {
        assertThatThrownBy(() -> FacetPathCodec.splitSlug("customer.city."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank segment");
    }

    @Test
    void splitSlugRejectsDoubledDot() {
        assertThatThrownBy(() -> FacetPathCodec.splitSlug("customer..city"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank segment");
    }

    @Test
    void splitSlugOfFormatSlugRecoversSegmentSlugs() {
        FacetPath path = traversingPath();

        List<String> recovered = FacetPathCodec.splitSlug(FacetPathCodec.formatSlug(path));

        assertThat(recovered).containsExactly("customer", "address", "city");
    }
}
