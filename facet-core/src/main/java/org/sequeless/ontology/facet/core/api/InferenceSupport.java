package org.sequeless.ontology.facet.core.api;

/**
 * Whether a facet's data may come only from asserted statements, or may also be satisfied by the
 * inference graph.
 *
 * <p>Per design.md section 4.6, inference is <em>flagged, not executed</em>: this contract never
 * runs a reasoner and never tags result rows as inferred or asserted. All {@code
 * InferenceSupport} does is let a caller who asks for asserted-only data be told, up front, that a
 * given path will not behave as expected — before they build a query around it rather than after.
 * Actually reading the separate inference graph and tagging rows is the storage adapter's job, not
 * this contract's.
 */
public enum InferenceSupport {

    /** Only asserted statements satisfy this facet. */
    ASSERTED_ONLY,

    /** Data in the inference graph may also satisfy this facet, in addition to assertions. */
    MAY_BE_INFERRED
}
