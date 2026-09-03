package org.sequeless.ontology.facet.core.api;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * What to ask a {@code ConceptSource} for: the two ways a UI browses a concept scheme.
 *
 * <p>A concept picker is either a <em>search box</em> — the user types and sees matches, driven by
 * {@link #partialText()} — or a <em>tree</em>, where the user expands a node and sees its children,
 * driven by {@link #parentIri()}. Both modes read from the same scheme, so they share one query
 * type rather than two near-identical ports.
 *
 * @param partialText text the user has typed so far, matched against preferred and alternative
 *     labels; may be {@code null}, meaning "no text filter"
 * @param parentIri when present, list the children of this concept — the tree-picker mode; never
 *     {@code null} as a wrapper, use {@link Optional#empty()} to search the whole scheme instead
 * @param locale the locale to prefer when selecting labels; never {@code null} as a wrapper, use
 *     {@link Optional#empty()} to accept the scheme's default
 * @param limit the maximum number of concepts to return. A scheme may hold hundreds of thousands
 *     of concepts, so a picker always asks for a page rather than the whole taxonomy
 */
public record ConceptQuery(String partialText, Optional<String> parentIri, Optional<Locale> locale, int limit) {

    /** Validates the {@link Optional} wrappers. */
    public ConceptQuery {
        // partialText may be null: design.md section 3.2 — it means "no text filter"
        Objects.requireNonNull(parentIri, "parentIri must not be null (use Optional.empty())");
        Objects.requireNonNull(locale, "locale must not be null (use Optional.empty())");
    }
}
