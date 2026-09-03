# 0002. facet-core is a zero-dependency contract module

- Status: accepted
- Date: 2026-09-03

## Context

`sequeless-ontology` knows the domain schema; `sequeless-filter` turns human-typed text into a
filter AST against that schema. To do its job, filter must know what can be filtered, what type
each thing is, and what operators are legal on it — knowledge that in the real product is derived
from a compiled ontology and varies by tenant, by which ontology versions are in scope, and by how
far the caller is willing to traverse.

Neither library may depend on the other. `sequeless-filter` must not pull in RDF4J or any
ontology-loading machinery, and `sequeless-ontology` must not know how filter expressions are
parsed. But both need to agree on a vocabulary for "what can be filtered". Putting that vocabulary
in either repository makes the other depend on it — and, transitively, on whatever that repository
happens to depend on. The isolation collapses the first time either side adds a real dependency.

The normative design for this vocabulary is `docs/specs/facet-contract/design.md`, copied verbatim
from `sequeless-filter/docs/specs/ontology/design.md` so that both repositories build against one
agreed text.

A literal reading of that design puts the reusable contract test kit in `facet-core/src/test`,
running against an in-memory `FacetSource`. That does not work: the in-memory implementation
depends on `facet-core`'s own types, so exercising it from inside `facet-core` is a Maven reactor
cycle.

## Decision

We will add `facet-core` (`org.sequeless:ontology-facet-core`) as its own reactor module,
publishing `api`, `spi`, and `internal` packages under `org.sequeless.ontology.facet.core`, and
enforce at the build level — not by convention — that it never gains a compile- or runtime-scope
dependency. A module-local Enforcer execution, `ban-all-runtime-dependencies`, bans
`*:*:*:*:compile` and `*:*:*:*:runtime` transitively. Its distinct execution id merges it with the
parent's inherited `enforce-rules` and `ban-spring` rather than replacing them. Both
`sequeless-ontology` and `sequeless-filter` depend on `facet-core`; neither depends on the other.

The merge, stamp, and path-codec logic lives in `facet-core`'s `internal` package rather than in
each implementation, so that every `FacetSource` gets identical semantics for free instead of each
one reinventing — and getting subtly different — answers to "what happens when v1 and v2 disagree".

The reusable contract test kit ships in a second module, `facet-core-testfixtures`, alongside an
in-memory reference implementation, rather than in `facet-core/src/test`. This breaks the cycle:
the kit depends on `facet-core`, and `facet-core-testfixtures`'s own tests run the kit against the
in-memory source, validating the kit and the fixture together.

## Consequences

- **The zero-dependency rule is machine-enforced, not merely documented.** Adding any compile- or
  runtime-scope dependency to `facet-core/pom.xml` fails `make verify` on that rule, with a message
  pointing at the spec. This was verified by adding one and confirming the failure.
- **The parent's global `<dependencies>` are compatible with the rule.** Lombok is `provided` and
  JUnit/AssertJ/Mockito/ArchUnit are `test`; neither scope is banned, so `facet-core` inherits them
  without violating anything and `dependency:tree` still shows no compile or runtime entry.
- **`facet-core-testfixtures` is a published test-support artifact, not test-scoped code.** Its
  contract kit and in-memory fixture live in `src/main` so that any implementation — including the
  RDF-backed one that lands later, and consumers outside this reactor — can depend on it to
  validate itself. It therefore carries `junit-jupiter` and `assertj-core` at compile scope, which
  is correct for what it is and is why the zero-dependency rule is scoped to `facet-core` alone.
- **A module may not be listed in `<modules>` before its POM exists.** Maven resolves the reactor
  module graph before any lifecycle phase, so a listed module without a POM hard-fails every
  invocation. `facet-core-testfixtures` therefore enters `<modules>` in the same change that
  creates its POM, not earlier.
- **`facet-core` shares a repository with `ontology-core` but not a package tree or a dependency.**
  It must never import from `org.sequeless.ontology.api`/`spi`/`internal`. The Enforcer rule cannot
  see this — it only knows Maven coordinates — so an ArchUnit rule in `facet-core` asserts that the
  module depends on nothing outside `java..` and its own packages.
