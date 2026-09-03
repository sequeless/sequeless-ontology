package org.sequeless.ontology.facet.core.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Thrown when a facet scope, expansion, or path description cannot be resolved.
 *
 * <p>Per design.md section 2.7, resolution never returns a partial or silently-narrowed answer: it
 * either succeeds or throws. Returning a shortened facet list on failure would leave the caller
 * unable to tell "this class genuinely has three filterable properties" apart from "something went
 * wrong and you are seeing three of thirty", and a filter builder rendered from the latter is
 * worse than an error message.
 *
 * <p>The failure therefore carries a {@link FacetErrorCode} the caller can branch on and localize,
 * rather than only a message string. It also carries the {@link FacetPath} and {@link ClassRef}
 * involved where they are known, so a UI can point at the offending segment instead of rejecting
 * the whole expression.
 *
 * <p>{@code equals} and {@code hashCode} are deliberately not overridden. Two separate failures
 * carrying the same code are distinct events, and identity semantics inherited from
 * {@link Throwable} are the correct model for that.
 */
public final class FacetResolutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final FacetErrorCode code;
    private final transient Optional<FacetPath> path;
    private final transient Optional<ClassRef> classRef;

    /**
     * Creates a failure with no known path or class — used for whole-request failures such as
     * {@link FacetErrorCode#UNKNOWN_TENANT} or {@link FacetErrorCode#EMPTY_INTERSECTION}.
     *
     * @param code why resolution failed; never {@code null}
     */
    public FacetResolutionException(FacetErrorCode code) {
        this(code, Optional.empty(), Optional.empty());
    }

    /**
     * Creates a failure at a known path — used for path-shaped failures such as
     * {@link FacetErrorCode#UNKNOWN_PATH} or {@link FacetErrorCode#PATH_NOT_FILTERABLE}.
     *
     * @param code why resolution failed; never {@code null}
     * @param path where it went wrong; never {@code null}
     */
    public FacetResolutionException(FacetErrorCode code, FacetPath path) {
        this(code, Optional.of(Objects.requireNonNull(path, "path must not be null")), Optional.empty());
    }

    /**
     * Creates a failure at a known class — used for class-shaped failures such as
     * {@link FacetErrorCode#UNKNOWN_CLASS} or {@link FacetErrorCode#CLASS_NOT_REACHABLE}.
     *
     * @param code why resolution failed; never {@code null}
     * @param classRef the class involved; never {@code null}
     */
    public FacetResolutionException(FacetErrorCode code, ClassRef classRef) {
        this(code, Optional.empty(), Optional.of(Objects.requireNonNull(classRef, "classRef must not be null")));
    }

    /**
     * Creates a failure with both a path and a class — used where the offending segment and the
     * class it reaches are both meaningful, such as {@link FacetErrorCode#CYCLE_DETECTED} or
     * {@link FacetErrorCode#PRINCIPAL_DENIED}.
     *
     * @param code why resolution failed; never {@code null}
     * @param path where it went wrong; never {@code null}
     * @param classRef the class involved; never {@code null}
     */
    public FacetResolutionException(FacetErrorCode code, FacetPath path, ClassRef classRef) {
        this(
                code,
                Optional.of(Objects.requireNonNull(path, "path must not be null")),
                Optional.of(Objects.requireNonNull(classRef, "classRef must not be null")));
    }

    private FacetResolutionException(FacetErrorCode code, Optional<FacetPath> path, Optional<ClassRef> classRef) {
        super(buildMessage(Objects.requireNonNull(code, "code must not be null"), path, classRef));
        this.code = code;
        this.path = path;
        this.classRef = classRef;
    }

    private static String buildMessage(FacetErrorCode code, Optional<FacetPath> path, Optional<ClassRef> classRef) {
        StringBuilder message = new StringBuilder(code.name());
        path.ifPresent(p -> message.append(" at path ").append(p.asSlugPath()));
        classRef.ifPresent(c -> message.append(" for class ").append(c.slug()));
        return message.toString();
    }

    /**
     * Why resolution failed, as a constant the caller can branch on and localize.
     *
     * @return the error code; never {@code null}
     */
    public FacetErrorCode code() {
        return code;
    }

    /**
     * Where resolution went wrong, when a specific path was involved.
     *
     * @return the offending path, or empty for whole-request failures
     */
    public Optional<FacetPath> path() {
        return path;
    }

    /**
     * The class involved, when one was identified.
     *
     * @return the class, or empty when no single class is implicated
     */
    public Optional<ClassRef> classRef() {
        return classRef;
    }
}
