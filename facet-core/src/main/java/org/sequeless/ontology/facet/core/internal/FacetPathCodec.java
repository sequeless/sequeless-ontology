package org.sequeless.ontology.facet.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.sequeless.ontology.facet.core.api.FacetPath;

/**
 * Converts {@link FacetPath} to and from the two textual forms design.md section 2.1 describes:
 * the UUID form used on the wire and for persistence, and the slug form typed and displayed by a
 * human.
 *
 * <p><b>Slug in text, UUID on the wire — and that asymmetry is the whole point of this class.</b>
 * {@link #parseIds(String)} undoing {@link #formatIds(FacetPath)} is a faithful round trip:
 * {@code parseIds(formatIds(p))} equals {@code p.asIdPath()}, because the UUID form <em>is</em>
 * durable identity, and nothing is lost converting to text and back.
 *
 * <p>{@link #splitSlug(String)} undoing {@link #formatSlug(FacetPath)} is not that. It recovers
 * only the list of segment slugs as they read <em>right now</em> — not a {@link FacetPath}, because
 * a bare slug string cannot name a property's UUID, only whatever currently carries that slug. And
 * even that partial recovery is only meaningful at the instant it is read: a later rename changes
 * what the same slug string means, which is exactly why {@link #asIdPath()} rather than {@link
 * FacetPath#asSlugPath()} is what gets persisted. That is why this method is named {@code
 * splitSlug} rather than {@code parseSlug} — it performs a lexical split, not a parse that
 * recovers identity.
 *
 * @see FacetPath#asIdPath()
 * @see FacetPath#asSlugPath()
 */
public final class FacetPathCodec {

    private FacetPathCodec() {}

    /**
     * Renders {@code path} as its property UUIDs joined with {@code "/"}, the durable wire form.
     *
     * @param path the path to render
     * @return the UUIDs of {@code path.asIdPath()}, joined with {@code "/"}
     */
    public static String formatIds(FacetPath path) {
        StringBuilder result = new StringBuilder();
        List<UUID> ids = path.asIdPath();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                result.append('/');
            }
            result.append(ids.get(i));
        }
        return result.toString();
    }

    /**
     * Parses {@code idPath} — the inverse of {@link #formatIds(FacetPath)} — into an ordered list
     * of property UUIDs. {@code parseIds(formatIds(p))} equals {@code p.asIdPath()}: this round
     * trip is faithful because the UUID form is durable identity, not presentation.
     *
     * @param idPath the {@code "/"}-joined UUID path to parse; must not be blank, and no segment
     *     between slashes may be blank
     * @return the parsed UUIDs, in order, as an immutable list
     * @throws IllegalArgumentException if {@code idPath} is blank, if any segment between slashes
     *     is blank, or (propagated from {@link UUID#fromString(String)}) if any segment is not a
     *     well-formed UUID literal. Malformed syntax of an agreed wire format is a caller bug, not
     *     a domain lookup failure, so this is {@code IllegalArgumentException} rather than {@code
     *     FacetResolutionException}.
     */
    public static List<UUID> parseIds(String idPath) {
        if (idPath.isBlank()) {
            throw new IllegalArgumentException("idPath must not be blank");
        }
        String[] tokens = idPath.split("/", -1);
        List<UUID> ids = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (token.isBlank()) {
                throw new IllegalArgumentException("idPath must not contain a blank segment: '" + idPath + "'");
            }
            ids.add(UUID.fromString(token));
        }
        return List.copyOf(ids);
    }

    /**
     * Renders {@code path} as dot-joined current slugs. Delegates to {@link FacetPath#asSlugPath()}
     * so that there is exactly one place — the model type itself — that decides how a path reads as
     * text.
     *
     * @param path the path to render
     * @return {@code path.asSlugPath()}
     */
    public static String formatSlug(FacetPath path) {
        return path.asSlugPath();
    }

    /**
     * Splits {@code slugPath} into its dot-separated segment slugs. This is a <b>lexical split, not
     * a parse</b>: the result is a list of strings that were current slugs at some point, not a
     * {@link FacetPath}, because a bare slug cannot name a property's durable UUID. See the class
     * Javadoc for why {@code splitSlug(formatSlug(p))} does not, and cannot, round-trip back to
     * {@code p}.
     *
     * <p>Splits with {@code slugPath.split("\\.", -1)}. The {@code -1} limit is required so that
     * trailing empty tokens are retained by {@code split} rather than silently dropped, which is
     * what lets this method reject them explicitly instead of quietly accepting a malformed path
     * like {@code "customer."}.
     *
     * @param slugPath the {@code "."}-joined slug path to split; must not be blank, and no segment
     *     between dots may be blank
     * @return the segment slugs, in order, as an immutable list
     * @throws IllegalArgumentException if {@code slugPath} is blank, or if any segment is blank —
     *     which is what catches a leading dot, a trailing dot, and a doubled dot alike, all in one
     *     check
     */
    public static List<String> splitSlug(String slugPath) {
        if (slugPath.isBlank()) {
            throw new IllegalArgumentException("slugPath must not be blank");
        }
        String[] tokens = slugPath.split("\\.", -1);
        List<String> slugs = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (token.isBlank()) {
                throw new IllegalArgumentException("slugPath must not contain a blank segment: '" + slugPath + "'");
            }
            slugs.add(token);
        }
        return List.copyOf(slugs);
    }
}
