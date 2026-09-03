package org.sequeless.ontology.facet.core.api;

/**
 * Four independent signals about what may be done with a facet's values: filtered, faceted (i.e.
 * bucketed and counted), searched, and sorted.
 *
 * <p>Per design.md section 2.3, these are four <b>independent</b> booleans, not a hierarchy — none
 * of them implies another. In particular, {@link #filterable()} does not imply {@link
 * #facetable()}: a property can be perfectly safe to filter on while being a terrible candidate
 * for a checkbox-with-counts UI, because faceting only makes sense when the value's cardinality is
 * low enough that the resulting bucket list is useful rather than noise.
 *
 * <p>Worked example from design.md section 2.3, an {@code Invoice} class:
 *
 * <table>
 *   <caption>Invoice capability worked example</caption>
 *   <tr><th>Property</th><th>filterable</th><th>facetable</th><th>searchable</th><th>sortable</th></tr>
 *   <tr><td>{@code status}</td><td>yes</td>
 *       <td>yes &mdash; 6 values, render checkboxes with counts</td><td>no</td><td>yes</td></tr>
 *   <tr><td>{@code total}</td><td>yes</td>
 *       <td><b>no</b> &mdash; every value is distinct; bucketing produces noise</td>
 *       <td>no</td><td>yes</td></tr>
 *   <tr><td>{@code description}</td><td>yes</td>
 *       <td><b>no</b> &mdash; millions of buckets of size one</td><td>yes</td><td>no</td></tr>
 *   <tr><td>{@code internalNotes}</td><td>yes</td><td>no</td><td>no</td><td>no</td></tr>
 * </table>
 *
 * <p>Collapsing these four signals into a single flag would mean every consumer has to re-derive
 * "is this safe to build a checkbox list from?" by guessing at cardinality from whatever data
 * happens to be on screen — and getting it wrong on the field with 40,000 distinct values, where
 * the guess looks fine on a sample and then produces a useless wall of buckets in production.
 *
 * @param filterable whether this facet may appear in a filter expression
 * @param facetable whether this facet's cardinality is low enough to bucket-and-count, e.g. render
 *     as a checkbox list with per-bucket counts
 * @param searchable whether this facet participates in free-text search
 * @param sortable whether this facet may be used as a sort key
 */
public record Capabilities(boolean filterable, boolean facetable, boolean searchable, boolean sortable) {}
