package org.sequeless.ontology.facet.core.api;

import java.util.Objects;

/**
 * A single piece of human-facing text paired with the language it is written in.
 *
 * <p>Labels and descriptions throughout this contract ({@link ClassRef#label()}, a facet's label
 * and description, and so on) are {@code LocalizedText} rather than a bare {@code String} because
 * the ontology is not assumed to be authored in a single language. See design.md section 2.4.
 *
 * @param value the text itself; never {@code null}
 * @param languageTag a BCP 47 language tag (for example {@code "en"} or {@code "fr-CA"}), or
 *     {@code null} when the text is not tagged to a particular language. design.md section 2.4
 *     records this field as nullable, unlike every other reference field in this package.
 */
public record LocalizedText(String value, String languageTag) {

    /**
     * Validates that {@code value} is present. {@code languageTag} is deliberately not validated
     * here — it may be {@code null}, and when present this contract does not police it against the
     * BCP 47 grammar; that is a concern for whoever renders it.
     */
    public LocalizedText {
        Objects.requireNonNull(value, "value must not be null");
        // languageTag may be null: design.md section 2.4
    }
}
