package org.sequeless.ontology.facet.core.api;

/**
 * The closed set of value shapes a facet may hold.
 *
 * <p>Per design.md section 2.2, this is a contract-owned enum rather than XSD datatype IRIs or a
 * JSON Schema {@code type}/{@code format} pair, for reasons that matter to both sides of the
 * contract:
 *
 * <ul>
 *   <li><b>Not XSD IRIs.</b> Using raw XSD IRIs would force {@code sequeless-filter}'s operator
 *       derivation to pattern-match IRI strings and would leave it unable to handle a datatype it
 *       has never seen.
 *   <li><b>Not JSON Schema.</b> JSON Schema has no native date, decimal, IRI, or language-tagged
 *       string type; each of those would end up smuggled through a {@code format} string and
 *       parsed back out by every consumer independently.
 * </ul>
 *
 * <p>A closed enum lets the ontology add a custom datatype without breaking {@code
 * sequeless-filter}, and lets {@code sequeless-filter} add an operator without the ontology
 * needing to know. The native datatype IRI is never lost — it travels alongside on a facet's
 * {@code nativeDatatypeIri} for anyone who needs full fidelity — but no consumer is required to
 * understand it.
 */
public enum FacetType {

    /** An untagged text value. */
    STRING,

    /** A text value tagged with a language, i.e. backed by {@link LocalizedText}. */
    LANG_STRING,

    /** A whole number. */
    INTEGER,

    /** A fixed- or arbitrary-precision decimal number. */
    DECIMAL,

    /** A true/false value. */
    BOOLEAN,

    /** A calendar date without a time component. */
    DATE,

    /** A calendar date with a time component. */
    DATETIME,

    /** A time of day without a date component. */
    TIME,

    /** A span of time, such as an ISO-8601 duration. */
    DURATION,

    /** An IRI reference to a resource that is not a concept from a concept scheme. */
    IRI_REF,

    /** A reference to a {@code Concept} in a named concept scheme; see {@code ConceptSchemeRef}. */
    CONCEPT_REF,

    /** A geospatial value. */
    GEO,

    /** Opaque binary data. */
    BINARY,

    /**
     * The required escape hatch. An ontology may legitimately use a custom datatype this enum has
     * never heard of; such facets are still listed by {@code FacetSource} and still carry their
     * {@code nativeDatatypeIri} — they simply derive a minimal operator set ({@code exists} /
     * {@code does not exist}; design.md section 5) because nothing more specific can be assumed
     * about them.
     */
    UNKNOWN
}
