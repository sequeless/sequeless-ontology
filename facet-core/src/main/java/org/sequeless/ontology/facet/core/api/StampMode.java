package org.sequeless.ontology.facet.core.api;

/**
 * How a {@code FacetScope}'s freshness {@code stamp} is composed.
 *
 * <p>Per design.md section 4.9, the {@code stamp} field on a resolved scope is <em>always
 * present</em>; only its composition varies with this enum. Always emitting the field is the
 * point: if the stamp were absent under the cheap mode, tightening a deployment to {@link
 * #CONTENT_HASH} later would be a breaking change, and saved searches written in the meantime
 * would have nothing recorded to compare against. A saved search records the stamp it was built
 * against, so comparing stamps answers "was this search written against a different schema?"
 * before running it, rather than discovering breakage at query time.
 *
 * <p><b>Caution</b> (design.md section 4.9): enabling {@code TraversalConfig.rbacFiltersFacets}
 * while still using {@link #VERSION_ONLY} lets a cache keyed on the stamp serve one principal's
 * filtered facet list to another, because the stamp does not vary with the principal. Enabling
 * RBAC facet filtering should force {@link #CONTENT_HASH}, or the cache must key on the request
 * rather than on the stamp alone.
 */
public enum StampMode {

    /**
     * The stamp is the ontology version IRIs in scope, sorted and joined. Simple, and correct as
     * long as nothing besides the versions varies between two resolutions that should compare
     * equal. This is the default.
     */
    VERSION_ONLY,

    /**
     * The stamp is a hash over the ontology versions, the traversal configuration, the version
     * policy, the tenant, and the principal. Correct when any of those may vary per call — in
     * particular, required for correctness once RBAC facet filtering is enabled.
     */
    CONTENT_HASH
}
