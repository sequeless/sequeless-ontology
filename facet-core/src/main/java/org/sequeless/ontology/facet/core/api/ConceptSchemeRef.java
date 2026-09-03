package org.sequeless.ontology.facet.core.api;

import java.util.Objects;

/**
 * A reference to a named concept scheme that a {@code CONCEPT_REF}-typed facet draws its values
 * from.
 *
 * <p>Per design.md section 4.7, the scheme is named by {@link #schemeId()} — a stable id — and is
 * deliberately <b>not</b> version-pinned. This is what lets new concepts become filterable the
 * moment the scheme publishes them, with no ontology release required at all: the scheme has its
 * own clock, separate from the ontology's version history, precisely so that adding a concept
 * (say, a new product category) does not require redeploying or re-versioning the ontology.
 *
 * <p>{@link #hierarchical()} is what unlocks subtree operators. When {@code true}, an expression
 * such as {@code category is under 'Electronics'} matches records tagged with {@code Electronics}
 * or any descendant, such as {@code Laptops} or {@code Laptops/Ultrabooks}. Without the flag, the
 * facet is treated as an opaque IRI and only exact match is offered.
 *
 * @param schemeId the stable, non-version-pinned identity of the concept scheme; never {@code
 *     null} or blank
 * @param slug the current short name of the scheme, used for presentation; never {@code null} or
 *     blank
 * @param hierarchical whether the scheme's concepts form a hierarchy, which unlocks subtree
 *     ("is under") operators (design.md section 4.7)
 */
public record ConceptSchemeRef(String schemeId, String slug, boolean hierarchical) {

    /** Validates that both identity and presentation fields are present and non-blank. */
    public ConceptSchemeRef {
        Objects.requireNonNull(schemeId, "schemeId must not be null");
        if (schemeId.isBlank()) {
            throw new IllegalArgumentException("schemeId must not be blank");
        }
        Objects.requireNonNull(slug, "slug must not be null");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
    }
}
