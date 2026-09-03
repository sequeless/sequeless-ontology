package org.sequeless.ontology.facet.core.api;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A dot-walked path from a root class, expressed as an ordered list of {@link PathSegment}s.
 *
 * <p><b>Slug in text, UUID on the wire.</b> A user types {@code customer.city}. Per design.md
 * section 2.1, the parsed and persisted form of that path is the list of property UUIDs returned
 * by {@link #asIdPath()} — never the slugs. Rendering the path back to text with
 * {@link #asSlugPath()} uses whatever slug is <em>current</em> on each segment, so a saved search
 * written before a property rename still displays correctly after it: the identity survived the
 * rename even though the text did not.
 *
 * @param segments the ordered hops of the path, root-first; never empty, and every non-terminal
 *     segment (every segment but the last) must carry a non-null {@link PathSegment#targetClass()}
 *     because the path continues through it
 */
public record FacetPath(List<PathSegment> segments) {

    /**
     * Copies {@code segments} defensively (which also null-checks the list and every element),
     * rejects an empty path, and enforces that every segment before the last one is non-terminal —
     * i.e. actually leads somewhere further, rather than dead-ending in a literal value partway
     * through the path.
     */
    public FacetPath {
        segments = List.copyOf(segments); // null-checks the list and every element
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("segments must not be empty");
        }
        for (int i = 0; i < segments.size() - 1; i++) {
            if (segments.get(i).targetClass() == null) {
                throw new IllegalArgumentException("segment %d ('%s') is non-terminal and must have a targetClass"
                        .formatted(i, segments.get(i).slug()));
            }
        }
    }

    /**
     * Renders this path as dot-joined current slugs, for example {@code "customer.address.city"}.
     * This is the human-facing form; it is never the persisted form (see {@link #asIdPath()}).
     *
     * @return the slugs of every segment, in order, joined with {@code "."}
     */
    public String asSlugPath() {
        return segments.stream().map(PathSegment::slug).collect(Collectors.joining("."));
    }

    /**
     * Renders this path as the ordered list of property UUIDs. This is the durable form used for
     * persistence — a saved search stores this, not {@link #asSlugPath()}, precisely so that a
     * later rename does not break it. See design.md section 2.1.
     *
     * @return the property UUIDs of every segment, in order
     */
    public List<UUID> asIdPath() {
        return segments.stream().map(PathSegment::propertyId).toList(); // Stream.toList() is unmodifiable
    }

    /**
     * Whether this path crosses a relationship into another class, as opposed to naming a
     * property directly on the root.
     *
     * @return {@code true} when this path has more than one segment
     */
    public boolean isTraversing() {
        return segments.size() > 1;
    }

    /**
     * The number of hops in this path, counting the root's own property as depth 1.
     *
     * @return {@link #segments()}{@code .size()}
     */
    public int depth() {
        return segments.size();
    }
}
