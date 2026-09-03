# Facet Contract — design

**Status:** draft
**Consumers:** `sequeless-filter` (reads), `sequeless-ontology` (implements)
**Artifact:** `org.sequeless:ontology-facet-core`, packages `org.sequeless.ontology.facet.core.{api,spi,internal}`

---

## 1. Purpose and boundary

`sequeless-filter` turns human-typed text into a filter AST. To do that it must know, for a given
query, **what can be filtered on, what those things are, and what operators are legal on each**.
Today it knows this from a hand-built list of `FieldDefinition`s. In the real product that
knowledge is derived from a compiled ontology, and it is not static: it depends on the tenant, on
which ontology versions are in scope, and on how far the caller is willing to traverse.

This contract is the seam. It is a **zero-dependency Java module** that both sides depend on and
neither side owns the other through. `sequeless-ontology` implements the ports; `sequeless-filter`
consumes the model. Neither library appears on the other's dependency graph.

### In scope

- Describing a **facet**: a filterable path expression, its type, its cardinality, its
  capabilities, its labels, and the ontology versions that define it.
- Describing **walkable relationships** so paths can be extended across records.
- **Resolving a scope**: given a tenant, a root class, a version policy and a traversal
  configuration, produce the facets and relationships available.
- **Validating** a single path expression and explaining precisely why it is not valid.
- Naming the **concept scheme** a facet draws from, and whether that scheme is hierarchical.

### Explicitly out of scope

| Not here | Where it belongs |
|---|---|
| Operator names and their semantics | `sequeless-filter` derives them (§5) |
| SQL, SPARQL, or any query text | storage adapters |
| Record projection / result row shape | the query layer |
| Pagination and sorting *execution* | the query layer (the contract only flags `sortable`) |
| The RBAC row predicate itself | the query layer; this contract only optionally *hides* facets |
| Inference execution and result tagging | the storage adapter (the contract only flags §4.6) |
| Concept **values** | a separate port, `ConceptSource` (§3.2) — different release clock |
| Wire serialization | deliberately absent; see §6 |

### The one-sentence version

> Given *who is asking*, *what kind of record*, and *which schema versions count*, hand back the
> paths that can be filtered and enough facts about each to decide what may be done with it.

---

## 2. Model

Package `org.sequeless.ontology.facet.core.api`. Everything here is an immutable record or a
closed enum. No behavior beyond derived accessors and validation in compact constructors.

### 2.1 Identity and paths

Classes and properties carry an **immutable UUID** distinct from their slug and label. The UUID is
identity; the slug is presentation. This is what lets a property be renamed without breaking
anything that referenced it.

```java
public record ClassRef(UUID id, String slug, String iri, LocalizedText label) {}

public record PathSegment(
        UUID propertyId,
        String slug,
        ClassRef targetClass) {}   // null when this segment ends in a literal value

public record FacetPath(List<PathSegment> segments) {
    public String asSlugPath();        // "customer.address.city"
    public List<UUID> asIdPath();      // the durable form, for persistence
    public boolean isTraversing();     // segments.size() > 1
    public int depth();                // segments.size()
}
```

**Slug in text, UUID on the wire.** A user types `customer.city`. The parsed and persisted form is
the UUID list. Rendering back to text uses whatever slug is *current*, so a saved search written
before a rename displays correctly after it.

### 2.2 Type vocabulary

A closed, contract-owned enum. The native datatype IRI travels alongside for anyone who needs
full fidelity, but no consumer is required to understand it.

```java
public enum FacetType {
    STRING, LANG_STRING,
    INTEGER, DECIMAL,
    BOOLEAN,
    DATE, DATETIME, TIME, DURATION,
    IRI_REF, CONCEPT_REF,
    GEO, BINARY,
    UNKNOWN
}
```

`UNKNOWN` is the required escape hatch: an ontology may legitimately use a custom datatype this
enum has never heard of. `UNKNOWN` facets are still listed and still carry their
`nativeDatatypeIri`; they simply derive a minimal operator set (§5).

**Why not XSD IRIs, and why not JSON Schema.** XSD would force `sequeless-filter`'s derivation
rules to pattern-match IRI strings and handle datatypes it cannot know about; JSON Schema has no
native date, decimal, IRI, or language-tagged string, so those would end up smuggled through a
`format` string and parsed back out. A closed enum lets the ontology add a custom datatype without
breaking filter, and lets filter add an operator without the ontology knowing.

### 2.3 Capabilities

Four independent booleans. They are not a hierarchy and none implies another.

```java
public record Capabilities(
        boolean filterable,   // may appear in a filter expression
        boolean facetable,    // low-cardinality enough to bucket-and-count
        boolean searchable,   // participates in free-text search
        boolean sortable) {}  // may be used as a sort key
```

Worked example — an `Invoice` class:

| Property | filterable | facetable | searchable | sortable |
|---|---|---|---|---|
| `status` | yes | yes — 6 values, render checkboxes with counts | no | yes |
| `total` | yes | **no** — every value is distinct; bucketing produces noise | no | yes |
| `description` | yes | **no** — millions of buckets of size one | yes | no |
| `internalNotes` | yes | no | no | no |

Collapsing these into one flag means every consumer re-derives "is this safe to build a checkbox
list from?" by guessing at cardinality, and gets it wrong on the field with 40 000 distinct values.

### 2.4 Facets

```java
public record Facet(
        FacetPath path,
        FacetType type,
        String nativeDatatypeIri,             // may be null
        Cardinality cardinality,              // SINGLE | MANY
        Capabilities capabilities,
        LocalizedText label,
        LocalizedText description,            // may be null
        Optional<ConceptSchemeRef> conceptScheme,
        OperatorRestriction restriction,
        InferenceSupport inference,
        Set<String> definedInVersions,        // version IRIs that define this facet
        Optional<FacetConflict> conflict) {}

public enum Cardinality { SINGLE, MANY }

public enum InferenceSupport { ASSERTED_ONLY, MAY_BE_INFERRED }

public record ConceptSchemeRef(
        String schemeId,        // stable id, NOT version-pinned
        String slug,
        boolean hierarchical) {}

public record LocalizedText(String value, String languageTag) {}  // languageTag may be null

public record OperatorRestriction(List<String> allowed, List<String> denied) {
    public static OperatorRestriction unrestricted();   // both empty
}
```

`OperatorRestriction` is how the ontology **narrows** what filter would otherwise derive (§5). It
can never widen. Both lists empty means "no opinion".

### 2.5 Relationships

Only relationships the ontology has marked **walkable** are ever returned. An unmarked
relationship is invisible to traversal — this is the primary guard against path explosion (§4.3).

```java
public record Relationship(
        UUID propertyId,
        String slug,
        LocalizedText label,
        ClassRef targetClass,
        Cardinality cardinality,
        Set<String> definedInVersions) {}
```

### 2.6 Requests and results

```java
public record FacetScopeRequest(
        String tenantId,
        ClassRef root,
        VersionPolicy versionPolicy,
        TraversalConfig traversal,
        Optional<Principal> principal,
        Optional<Locale> locale) {}

public sealed interface VersionPolicy {
    record Union() implements VersionPolicy {}                  // default
    record Pinned(String versionIri) implements VersionPolicy {}
    record Intersection() implements VersionPolicy {}
}

public record TraversalConfig(
        OptionalInt maxDepth,          // empty = unbounded; default = OptionalInt.of(3)
        boolean noRevisitClasses,      // default true
        StampMode stampMode,           // default VERSION_ONLY
        boolean rbacFiltersFacets) {}  // default false

public enum StampMode { VERSION_ONLY, CONTENT_HASH }

public record Principal(String id, Set<String> roles) {}

public record FacetScope(
        ClassRef root,
        List<Facet> facets,                 // eager at depth 1 (see §4.2)
        List<Relationship> relationships,   // walkable edges from the current frontier
        String stamp,
        Set<String> ontologyVersions,
        FacetScopeRequest request) {}       // echoed, so the scope can be re-expanded
```

**On the `maxDepth` default.** The contract supports unbounded traversal, but the *default* is
finite (3). Unbounded is a deliberate opt-in, because the three guards in §4.3 make deep traversal
safe, not cheap.

### 2.7 Errors

Resolution never returns a partial or silently-narrowed answer. It either succeeds or throws with
a code the caller can branch on and localize.

```java
public enum FacetErrorCode {
    UNKNOWN_TENANT,
    UNKNOWN_CLASS,
    UNKNOWN_PATH,          // no such property at this point in the path
    PATH_NOT_FILTERABLE,   // resolves, but capabilities.filterable() is false
    CLASS_NOT_REACHABLE,   // no walkable route from the root
    EDGE_NOT_WALKABLE,     // the property exists but is not marked walkable
    DEPTH_EXCEEDED,
    CYCLE_DETECTED,
    VERSION_NOT_FOUND,
    EMPTY_INTERSECTION,    // INTERSECTION policy left nothing
    PRINCIPAL_DENIED,      // only when rbacFiltersFacets is on
    SCHEME_UNAVAILABLE
}

public final class FacetResolutionException extends RuntimeException {
    public FacetErrorCode code();
    public Optional<FacetPath> path();     // where it went wrong
    public Optional<ClassRef> classRef();
}
```

`UNKNOWN_PATH` and `PATH_NOT_FILTERABLE` are deliberately distinct. "There is no such field" and
"that field exists but you may not filter on it" lead to different UI copy and different bug
reports.

---

## 3. Ports

Package `org.sequeless.ontology.facet.core.spi`.

### 3.1 FacetSource

```java
public interface FacetSource {

    /** Resolve a scope. Returns the root's own facets and its walkable relationships. */
    FacetScope resolve(FacetScopeRequest request);

    /** Extend an already-resolved scope one hop along the named relationship. */
    FacetScope expand(FacetScope scope, FacetPath relationship);

    /** Validate and describe exactly one path. Throws with a code if it does not resolve. */
    Facet describe(FacetScopeRequest request, FacetPath path);
}
```

Three methods, because there are three distinct questions and conflating them forces the
implementation to be either wasteful or incomplete:

- `resolve` powers first paint of a filter builder.
- `expand` powers "the user typed a dot after `customer`".
- `describe` powers re-validating a saved search without enumerating anything.

### 3.2 ConceptSource

Separate port, because concept schemes are published on their own clock (§4.7). Bundling concept
values into `Facet` would mean caching a taxonomy snapshot inside a facet response and serving it
stale.

```java
public interface ConceptSource {
    List<Concept> concepts(String schemeId, ConceptQuery query);
    Optional<Concept> concept(String schemeId, String conceptIri);
}

public record Concept(
        String iri,
        LocalizedText prefLabel,
        List<LocalizedText> altLabels,
        Optional<String> broaderIri,
        boolean hasNarrower) {}

public record ConceptQuery(
        String partialText,          // may be null
        Optional<String> parentIri,  // present = list children of this concept (tree picker)
        Optional<Locale> locale,
        int limit) {}
```

`hasNarrower` exists so a tree picker can render an expand arrow without a second round trip.

---

## 4. Semantics

### 4.1 Scope is a root class plus what is reachable

A query scope is **one root class**, plus whatever traversal reaches from it. There is no way to
name an unrelated set of classes, because there is no syntax for it.

- `Invoice` + `Customer` — legal. `Invoice.customer` is a walkable edge, so `customer.city`
  resolves from the `Invoice` root.
- `Person` + `Planet` — **impossible to express.** No edge connects them, so no path from a
  `Person` root reaches `Planet`.

Connectedness is therefore enforced by construction rather than by a validation rule that has to
be written, tested, and kept in sync. The requirement "several classes, but only if there is a
reasonable way to join them" *is* traversal.

### 4.2 Discovery: eager at depth 1, lazy beyond

`resolve` returns the root's own facets fully populated, plus the list of walkable relationships —
enough to render a complete filter builder for the common case with one call. Going deeper is an
explicit `expand`.

This is not an optimization; it is forced. Under unbounded traversal a flat list of every legal
path is infinite the moment a cycle exists, and even acyclic it is exponential in fan-out — 30
properties × 5 relationships × 30 properties is 4 500 entries at depth 2 alone.

```
resolve(root = Invoice)
  → facets:        [ number, status, total, issuedAt, description, ... ]
    relationships: [ customer → Customer, lineItems → LineItem ]

expand(scope, "customer")
  → facets:        [ ..., customer.name, customer.city, customer.tier ]
    relationships: [ ..., customer.account → Account ]
```

### 4.3 The three traversal guards

All three apply simultaneously. They fail independently, with distinct error codes.

**Guard 1 — edge opt-in (`EDGE_NOT_WALKABLE`).** A relationship is traversable only if the
ontology marks it so. This is the guard that does most of the work: most cycles simply never get
marked, and it puts the decision with the person who understands the domain.

**Guard 2 — no revisit (`CYCLE_DETECTED`).** While building a single path, never re-enter a class
already on that path. Kills `Invoice → customer → invoices → customer → …` mechanically, without
the ontology author having to notice the cycle exists.

```
Invoice → customer (Customer)     ok, Customer not yet on path
        → invoices (Invoice)      REJECTED — Invoice is already the root of this path
```

Note this is per-path, not global: `Invoice → customer` and `Invoice → lineItems → invoice` are
evaluated independently, and a class may appear in two different paths.

**Guard 3 — depth cap (`DEPTH_EXCEEDED`).** A ceiling that applies even when guards 1 and 2 pass,
so a wide acyclic graph cannot explode. Default 3; `OptionalInt.empty()` disables it.

### 4.4 Version policies

Records are stamped `dcterms:conformsTo <versionIRI>` and never auto-upgrade, so a scope routinely
spans versions. The policy is a **request parameter**, not a deployment setting.

Setup for the examples: `Invoice` in v1 and v2. Between them, `custName` was renamed to
`customerName` (same property UUID), and v2 added `region`.

**`Union` (default)** — merge by property UUID. Slug rename does not split the facet in two;
v2's slug wins for display, and `definedInVersions` records both.

```
facets: customerName  (was "custName" in v1)  definedInVersions = { v1, v2 }
        total                                  definedInVersions = { v1, v2 }
        region                                 definedInVersions = { v2 }
```

Filtering on `region` simply matches no v1 record. The UI can say so, because §4.5 tells it.

**`Pinned(v1)`** — exactly v1's schema. `custName` is the slug; `region` is absent and typing it
raises `UNKNOWN_PATH`. Use when a saved search must mean forever what it meant when written.

**`Intersection`** — only facets defined in every version in scope: `customerName` and `total`.
`region` is absent. Every filter is guaranteed to mean the same thing for every record in scope.
If nothing survives, `EMPTY_INTERSECTION`.

### 4.5 Merge conflicts

A conflict is: same property UUID, incompatible `type` or `cardinality` across versions in scope.
Say v1 typed `issuedAt` as `STRING` and v2 corrected it to `DATETIME`.

The facet is **kept**, marked conflicted, and carries the per-version detail:

```java
public record FacetConflict(
        List<ConflictingDefinition> definitions,
        String reason) {}

public record ConflictingDefinition(
        String versionIri,
        FacetType type,
        Cardinality cardinality) {}
```

```
issuedAt   type = STRING (widest common)   conflict = {
             { v1, STRING,   SINGLE },
             { v2, DATETIME, SINGLE },
             reason: "datatype changed between versions" }
```

Consumers must offer only operators valid for **every** conflicting definition — here that is
`is`, `is not`, `is in`, `exists`, `does not exist`, and not `>` or `between`. The query still
runs and still returns records from both versions.

The rejected alternatives are worse. Failing the whole resolution makes one bad property change
render an entire class unqueryable, in a system whose premise is that versions coexist.
Last-version-wins is silently incorrect — a `DATETIME` comparison against v1's string data either
errors deep in the backend or quietly matches nothing.

### 4.6 Cardinality and inference

**Multi-valued paths match on any value.** A record with `tags = [urgent, billing]` matches
`tags is 'urgent'`. This is what users expect and what every search UI does. `Cardinality.MANY` is
exposed so consumers can render a multi-select and so filter can offer explicit quantifiers
(`has all of`, `has none of`) only where they mean something.

**Inference** is flagged, not executed. `MAY_BE_INFERRED` means the facet can be satisfied by data
in the inference graph. That lets a caller asking for asserted-only data be told, up front, that
the path they picked will not behave as they expect. Reading the separate graph and tagging result
rows is the storage adapter's job.

### 4.7 Concept schemes

A concept-valued facet names its scheme by **stable id and does not pin a version**. New concepts
become filterable the moment the scheme publishes them — no ontology release required, which is
precisely why the scheme has its own clock.

`hierarchical = true` is what unlocks subtree operators. `category is under 'Electronics'` matches
records tagged with `Electronics` *or any descendant* — `Laptops`, `Laptops/Ultrabooks`. Without
the flag, filter treats the facet as an opaque IRI and offers only exact match.

A record referencing a concept since removed from the scheme still filters by IRI; it just no
longer autocompletes. This is correct — the record's data has not changed.

### 4.8 Tenancy

Ontologies are tenant-scoped. `tenantId` is required on every request and participates in the
cache key. With one tenant today this is one parameter that is always the same value — and it
costs nothing, versus a breaking change across every signature the first time a customer needs a
custom property.

Deliberately **not** designed now: a shared base layer with per-tenant extensions. That needs
merge order, slug-collision rules, and a decision about whether a base-version-stamped record is
still valid under a merged view. See §6.

### 4.9 Stamps and freshness

Every `FacetScope` carries a `stamp`. The field is always present; only its composition varies.

| Mode | Composition | Use |
|---|---|---|
| `VERSION_ONLY` (default) | ontology version IRIs, sorted and joined | simple; correct when nothing else varies |
| `CONTENT_HASH` | hash over versions + traversal config + version policy + tenant + principal | correct when config or RBAC filtering varies per call |

A saved search records the stamp it was built against. Comparing stamps answers "was this search
written against a different schema?" *before* running it, rather than discovering breakage at
query time.

**Always emitting the field is the point.** If the stamp were absent in the cheap mode, tightening
to `CONTENT_HASH` later would be a breaking change and saved searches written in the meantime
would have nothing recorded.

> **Caution.** With `rbacFiltersFacets` enabled and `VERSION_ONLY` stamps, a cache keyed on the
> stamp will serve one principal's filtered facet list to another. Enabling RBAC filtering should
> force `CONTENT_HASH`, or the cache must key on the request rather than the stamp.

### 4.10 Optional RBAC facet filtering

Default **off**: everyone sees the schema, and the row-level predicate does the work. This keeps
the cache key free of principal, which is a large win — one facet list per ontology version
instead of one per role.

When on, resolution takes a `Principal` and paths reaching a class they cannot read are simply
absent; typing one raises `PRINCIPAL_DENIED`. This prevents the facet list from disclosing the
schema, and prevents users building filters that silently return nothing.

---

## 5. Operator derivation reference (non-normative)

**This section describes what `sequeless-filter` does with the contract. It is not part of the
contract.** It lives here so an ontology author can see what marking a property `hierarchical` or
`MANY` actually buys them.

Derivation is three steps:

1. **Derive** from `FacetType` × `Cardinality` × `conceptScheme` using the table below.
2. **Restrict** by the facet's `OperatorRestriction` (`allowed` intersects, `denied` subtracts).
3. **Narrow to conflict-safe** — if `conflict` is present, keep only operators valid for every
   `ConflictingDefinition` (§4.5).

| `FacetType` | Derived operators |
|---|---|
| `STRING`, `LANG_STRING` | `is`, `is not`, `is in`, `contains`, `starts with`, `is like`, `is not like`, `exists`, `does not exist` |
| `INTEGER`, `DECIMAL` | `is`, `is not`, `>`, `>=`, `<`, `<=`, `is in`, `between`, `exists`, `does not exist` |
| `DATE`, `DATETIME`, `TIME`, `DURATION` | same as numeric |
| `BOOLEAN` | `is`, `is not`, `exists`, `does not exist` |
| `IRI_REF` | `is`, `is not`, `is in`, `exists`, `does not exist` |
| `CONCEPT_REF` | as `IRI_REF`; plus `is under`, `is not under` when `hierarchical` |
| `GEO`, `BINARY` | `exists`, `does not exist` |
| `UNKNOWN` | `exists`, `does not exist` |
| any with `Cardinality.MANY` | adds `has all of`, `has none of` |

Rules are injectable Java objects with these as the shipped defaults — the same extension idiom
`sequeless-filter` already uses for `OperatorContributor` and `FieldContributor`. No config file
format, no parser, no schema to version.

Worked example — `Invoice.category`, `CONCEPT_REF`, hierarchical, `MANY`, with
`OperatorRestriction(denied = ["is like"])`:

```
derive   → is, is not, is in, exists, does not exist, is under, is not under,
           has all of, has none of
restrict → unchanged ("is like" was never derived)
conflict → none
result   → the nine above
```

---

## 6. Non-goals and future work

**Shared base ontologies with per-tenant extensions.** Deferred deliberately (§4.8). Adding it
means specifying merge order, slug-collision handling, what a tenant may and may not override, and
whether a record stamped with a base version IRI is valid under a merged view. Worth doing when
sharing has a concrete requirement, not before.

**Concept scheme version pinning.** Currently latest-wins (§4.7). A per-query pin would give
reproducibility at the cost of a second resolution path. Additive when needed.

**A JSON wire representation.** The contract is in-process Java. If the ontology ever becomes a
separately deployed service, add serialization as a **sibling module** — never by putting Jackson
into `facet-core`. The moment the contract has a dependency, every consumer inherits it.

**Facet-level statistics.** Distinct-value counts would let a consumer decide `facetable` for
itself, and would help a query planner. It is a different kind of data with a different freshness
requirement, so it belongs behind its own port if it is ever wanted.
