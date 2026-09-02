# 0001. Adopt a Maven reactor with an api/spi/internal hexagon

- Status: accepted
- Date: 2026-09-02

## Context

`sequeless-ontology` starts empty: a fresh repository with no source and no history. A
sibling library, `sequeless-filter`, already encodes a house layout for a Sequeless Java
library — a Maven reactor whose root is simultaneously aggregator and parent, hexagonal
boundaries expressed as `api`/`spi`/`internal` packages, Conventional Commits driving
semantic-release, and Spotless/Enforcer/JaCoCo as the `verify` gates.

`sequeless-ontology` is planned as a core library publishing `api` and `spi` contracts for a
domain ontology model, plus a reference adapter (`ontology-owl-adapter`) translating that
model to and from OWL. The adapter must not inherit dependencies it doesn't need, and the
boundary between what the core library publishes and what it keeps internal must not be
something a future contributor can silently erode by importing across it. Because this is a
greenfield repository, we can adopt filter's proven layout directly rather than growing into
it later through a disruptive restructuring.

## Decision

We will adopt, at inception, a Maven reactor whose root `pom.xml` is simultaneously
aggregator (`<packaging>pom</packaging>`, listing the modules) and parent (holding the shared
version, dependency management, and build plugin configuration). Two jar modules sit under
it: `ontology-core` (the domain model and its published `api`/`spi` contracts) and
`ontology-owl-adapter` (a reference adapter depending only on `ontology-core`'s `api`).

Within each module we will express the hexagon as three packages — `api` (the published
contract and its default implementations), `spi` (extension points and driven ports that
consumers implement), and `internal` (implementation detail nothing outside the module may
depend on) — and enforce those boundaries mechanically with ArchUnit rather than with JPMS
(`module-info.java`).

## Consequences

- **`dependencyManagement` stays opt-in.** Dependencies such as `jackson-databind` are
  declared only in the parent's `<dependencyManagement>`; each module redeclares what it
  actually uses. This is what lets `ontology-owl-adapter` avoid inheriting transitive
  dependencies it has no use for — it depends on `ontology-core` and nothing else today.
- **Child modules omit `<version>`** and inherit it from the parent, so semantic-release
  bumps a single number for the whole reactor. This only works as long as
  `.releaserc.json` keeps **both** `-DprocessAllModules=true` (so `versions:set` rewrites
  every child's `<parent><version>`, not just the aggregator's own `<version>`) **and** the
  `*/pom.xml` asset glob (so the rewritten child POMs are actually committed as part of the
  release). Dropping either one leaves child `<parent>` version references stale after the
  next release commit.
- **`flatten-maven-plugin` sits in `pluginManagement`, declared bare only by the jar
  modules.** An aggregator has no artifact to flatten, so it must not bind the plugin itself;
  each jar module declares groupId/artifactId only and inherits the managed configuration
  (`ossrh` mode, `updatePomFile=true`, bound to `process-resources` and `clean`).
- **ArchUnit, not JPMS, enforces the `internal` boundary.** There is no
  `module-info.java` anywhere in the reactor; the guarantee that nothing outside a module
  reaches its `internal` package, and that `ontology-owl-adapter` never reaches
  `ontology-core`'s `internal`/`spi` packages, is a `BoundaryRulesTest` per module, run as
  part of `make verify`. This is weaker than JPMS at the classpath level but matches filter's
  convention and needs no module descriptors to maintain.
- **The scaffold's ArchUnit rules currently carry `.allowEmptyShould(true)`.** With no
  domain types yet, the only compiled classes are `package-info`, which the rules
  deliberately exclude — so every rule would otherwise match zero classes and fail on an
  empty build. Each such rule carries a `TODO` to drop the flag once real `api`/`spi`
  types land in these packages; leaving it in place after that point would silently weaken
  the guard it exists to provide.
