package org.sequeless.ontology.facet.core.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.stream.Collectors;
import org.sequeless.ontology.facet.core.api.FacetScope;
import org.sequeless.ontology.facet.core.api.FacetScopeRequest;
import org.sequeless.ontology.facet.core.api.Principal;
import org.sequeless.ontology.facet.core.api.TraversalConfig;
import org.sequeless.ontology.facet.core.api.VersionPolicy;

/**
 * Computes {@link FacetScope#stamp()} per design.md section 4.9.
 *
 * <p>The {@link FacetScopeRequest} passed to {@link #stamp(FacetScopeRequest, Set)} never carries
 * the resolved ontology versions itself — which versions actually contributed to a scope is a fact
 * only resolution can determine, since it depends on what is stored, not merely on what was asked
 * for. That is why {@code versions} is a separate parameter rather than something read off the
 * request.
 */
public final class StampCalculator {

    private StampCalculator() {}

    /**
     * Computes the stamp for a resolved scope, per {@link TraversalConfig#stampMode()} on {@code
     * request.traversal()}.
     *
     * @param request the request the scope was resolved from
     * @param versions the ontology version IRIs that actually contributed to the scope
     * @return the stamp to record on the resulting {@link FacetScope}; never blank
     */
    public static String stamp(FacetScopeRequest request, Set<String> versions) {
        return switch (request.traversal().stampMode()) {
            case VERSION_ONLY -> versionOnly(versions);
            case CONTENT_HASH -> contentHash(request, versions);
        };
    }

    /**
     * The cheap stamp: {@code versions} sorted naturally and joined with {@code ","}.
     *
     * <p>Per design.md section 4.9, this mode is correct exactly as long as nothing besides the
     * version set varies between two resolutions that should compare equal — it is deliberately
     * cheap, not deliberately complete; full injectivity over every input that could affect the
     * scope is what {@link #contentHash(FacetScopeRequest, Set)} is for.
     *
     * <p><b>Edge case.</b> When {@code versions} is empty, {@code String.join} would yield {@code
     * ""}, but {@link FacetScope}'s compact constructor rejects a blank stamp. This method returns
     * the literal sentinel {@code "unversioned"} in that case instead, deliberately: it is not a
     * real version IRI and can never collide with one (version IRIs are never bare lowercase
     * words without a scheme), so it unambiguously signals "no version-defined content is in
     * scope" rather than accidentally comparing equal to some other empty-ish stamp.
     */
    private static String versionOnly(Set<String> versions) {
        if (versions.isEmpty()) {
            return "unversioned";
        }
        return versions.stream().sorted().collect(Collectors.joining(","));
    }

    /**
     * The precise stamp: {@code "sha256:"} followed by the 64 lowercase hex digits of the SHA-256
     * digest of the canonical string's UTF-8 bytes.
     *
     * <p>This deliberately never uses {@link Object#hashCode()}: {@code hashCode} is not specified
     * to be stable across JVM runs (and for records composed of collections, is not in practice),
     * so it cannot be used anywhere the stamp must be compared later — including after a restart,
     * or between two different processes.
     */
    private static String contentHash(FacetScopeRequest request, Set<String> versions) {
        String canonical = canonicalString(request, versions);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return "sha256:" + hex;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every conforming JDK implementation
            // (see MessageDigest's algorithm table), so this is unreachable in practice.
            throw new IllegalStateException("SHA-256 is required to be available on every JDK", e);
        }
    }

    /**
     * Builds the canonical, injective string that {@link #contentHash(FacetScopeRequest, Set)}
     * hashes: ten fields, each length-prefixed as a netstring ({@code
     * <utf8ByteLength>:<value>}) and concatenated with no separator. Because every field is
     * self-delimiting by its own length prefix, no separator character could ever be mistaken for
     * part of a field's content — a tenant literally named {@code "a;b"} cannot collide with a
     * different input that happens to produce the same characters around a chosen separator.
     *
     * <p>Field order, matching the table in design.md section 4.9:
     *
     * <ol>
     *   <li>the literal discriminator {@code "CONTENT_HASH"} — this is what makes the hash differ
     *       from any {@code VERSION_ONLY} stamp that happened to hash the same underlying data, and
     *       is also why {@link TraversalConfig#stampMode()} itself is <b>not</b> a separate field:
     *       the fixed discriminator already encodes which mode produced this stamp.
     *   <li>{@code request.tenantId()}
     *   <li>{@code request.root().id()}, rendered via {@link Object#toString()} — included even
     *       though design.md section 4.9's table does not list it. Without it, two scopes rooted at
     *       different classes would produce identical stamps whenever they happened to share a
     *       version set, traversal config, tenant, and principal, which would defeat the stamp's
     *       stated purpose of answering "was this search written against a different schema?" This
     *       is a deliberate strengthening of the spec's table, not an oversight.
     *   <li>{@code versions}, sorted naturally, each netstringed and concatenated, then the whole
     *       concatenation netstringed again (netstring-of-netstrings) — {@code ""} if empty
     *   <li>the version policy, rendered as {@code "Union"}, {@code "Intersection"}, or {@code
     *       "Pinned:" + versionIri}
     *   <li>{@code maxDepth}, rendered as decimal digits when present, else the literal {@code
     *       "empty"}
     *   <li>{@code noRevisitClasses}, rendered as {@code "true"}/{@code "false"}
     *   <li>{@code rbacFiltersFacets}, rendered as {@code "true"}/{@code "false"}
     *   <li>the principal's id, or {@code ""} when the principal is absent
     *   <li>the principal's roles, sorted naturally, each netstringed and concatenated, then the
     *       whole concatenation netstringed again — {@code ""} when the principal is absent or has
     *       no roles
     * </ol>
     *
     * <p>{@code request.locale()} is deliberately excluded: locale selects which {@link
     * org.sequeless.ontology.facet.core.api.LocalizedText} value is displayed, not which facets or
     * relationships exist. Two requests differing only in locale describe exactly the same schema,
     * so folding locale into the hash would make semantically identical scopes stamp differently
     * for no reason tied to the scope's actual content.
     *
     * <p>Order-independence for {@code versions} and for the principal's roles comes from sorting
     * each set before netstringing it. Stability across JVM runs and processes comes from hashing
     * with SHA-256 rather than any JVM-local mechanism such as {@link Object#hashCode()}.
     */
    private static String canonicalString(FacetScopeRequest request, Set<String> versions) {
        StringBuilder canonical = new StringBuilder();
        canonical.append(netstring("CONTENT_HASH"));
        canonical.append(netstring(request.tenantId()));
        canonical.append(netstring(request.root().id().toString()));
        canonical.append(netstring(netstringSet(versions)));
        canonical.append(netstring(renderVersionPolicy(request.versionPolicy())));
        canonical.append(netstring(renderMaxDepth(request.traversal())));
        canonical.append(netstring(Boolean.toString(request.traversal().noRevisitClasses())));
        canonical.append(netstring(Boolean.toString(request.traversal().rbacFiltersFacets())));
        canonical.append(netstring(request.principal().map(Principal::id).orElse("")));
        canonical.append(
                netstring(netstringSet(request.principal().map(Principal::roles).orElse(Set.of()))));
        return canonical.toString();
    }

    private static String renderVersionPolicy(VersionPolicy versionPolicy) {
        return switch (versionPolicy) {
            case VersionPolicy.Union ignored -> "Union";
            case VersionPolicy.Intersection ignored -> "Intersection";
            case VersionPolicy.Pinned pinned -> "Pinned:" + pinned.versionIri();
        };
    }

    private static String renderMaxDepth(TraversalConfig traversal) {
        return traversal.maxDepth().isPresent()
                ? Integer.toString(traversal.maxDepth().getAsInt())
                : "empty";
    }

    private static String netstring(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length + ":" + s;
    }

    private static String netstringSet(Set<String> set) {
        return set.stream().sorted().map(StampCalculator::netstring).collect(Collectors.joining());
    }
}
