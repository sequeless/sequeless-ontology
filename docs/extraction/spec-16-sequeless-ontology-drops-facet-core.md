# Spec 16 — sequeless-ontology drops facet-core

Issue: [sequeless-prototype#44](https://github.com/sequeless/sequeless-prototype/issues/44). Map: [#11](https://github.com/sequeless/sequeless-prototype/issues/11). Plan: [#23](https://github.com/sequeless/sequeless-prototype/issues/23). Decisions: [#13](https://github.com/sequeless/sequeless-prototype/issues/13) (facet-core moves to sequeless-query). Earlier spec: [spec 14](https://github.com/sequeless/sequeless-query/blob/main/docs/extraction/spec-14-sequeless-query.md) (sequeless-prototype#42, closed), which copied the module into sequeless-query.

This spec is committed in the first code PR of this repo, as the run rules require. Spec 16 is not a new repo, so it adds no repo-level PROVENANCE file; the provenance record is in section 2.

## 1. Identity, stage and dependencies

- **Repo:** `sequeless/sequeless-ontology` (existing). Groups `org.sequeless`.
- **Stage 4** (#23 table). Waits for spec 14 (#42, closed). The move is complete on the query side, so this spec only removes the old copy.
- **What this spec removes:** the reactor modules `facet-core` (`org.sequeless:ontology-facet-core`) and `facet-core-testfixtures` (`org.sequeless:ontology-facet-core-testfixtures`), and the facet-contract design copy `docs/specs/facet-contract/design.md`. Neither module was ever released or published from this repo (section 6).
- **Dependency check (read against `origin/main` `c5c64a2`, 2026-11-26).** Grep for `facet-core`, `ontology-facet`, `ontology.facet`, `facet.core` and the facet type names across all nine sequeless repos. Code hits are listed here; docs and comments are not.

  | Repo | Code or config hits outside the facet modules | Result |
  | --- | --- | --- |
  | sequeless-ontology | `pom.xml` module lines; `sonar-project.properties` jacoco paths; `README.md` | Removed by this PR. |
  | sequeless-prototype | none | No change. It never had facet-core. |
  | sequeless-build | none; `sequeless-bom` lists no facet artifact | No change. |
  | sequeless-filter | none | No change. |
  | sequeless-kernel | `sonar-project.properties` jacoco path strings (template copy) | Config only, not a dependency. Follow-up. |
  | sequeless-expression | none | No change. |
  | sequeless-automation | `sonar-project.properties` jacoco path strings (template copy) | Config only. Follow-up. |
  | sequeless-template | `sonar-project.properties` path strings; a comment in `BoundaryRulesTest` | Config and comment only. Follow-up. |
  | sequeless-query | comments and test names only. Its only `org.sequeless.ontology` imports are `org.sequeless.ontology.schema` (schema-contract) | No facet dependency. |

  Result: no code in any repo depends on `facet-core`. The deletion needs no prerequisite refactor.

## 2. Moves

This spec moves nothing new. It deletes code that spec 14 already copied.

### 2.1 Deleted from this repo

- **Source repo:** `sequeless/sequeless-ontology`.
- **Source SHA:** `c5c64a2` (`origin/main` when the deletion was cut). The last change to `facet-core` or `facet-core-testfixtures` was `b38938d`, the commit that spec 14 read.
- **Paths deleted** (55 tracked files):
  - `facet-core/` (`pom.xml`; `src/main` for `api` (25 files), `spi` (3) and `internal` (4); `src/test` for `api` (4), `internal` (3) and the package root (1)).
  - `facet-core-testfixtures/` (`pom.xml`; `src/main` (10 files); `src/test` (2 files)).
  - `docs/specs/facet-contract/design.md`.
- **The move:** a deletion. Nothing is renamed or re-homed here.
- **Date:** 2026-11-26.

### 2.2 Provenance record

- **Destination:** `sequeless/sequeless-query`, which holds the copy. Its own `PROVENANCE.md` records the source as this repo at `b38938d`. The copy landed in query commit `7c65702` (`feat(query): the Field contract, spec 14 first code PR (#2)`, on query `main`).
- **Mapping in the destination:** `facet-core/src/main/java/org/sequeless/ontology/facet/core/{api,spi,internal}` became `query-field` (package `org.sequeless.query.field`), with `Facet*` renamed to `Field*`. `facet-core-testfixtures` became `query-field-testfixtures`. `design.md` became `docs/specs/field-contract.md`. The `Principal` type was dropped and replaced by kernel-identity's `Principal` (sequeless-query `PROVENANCE.md`).
- **Reverse pointer:** this PR's commit names `sequeless/sequeless-query` and `7c65702` in its body.

## 3. Prerequisite refactors

None. Section 1 shows that no code depends on the modules, so none needs rewiring before the deletion.

## 4. Package renames

None in this repo. The packages `org.sequeless.ontology.facet.core.{api,spi,internal,testfixtures}` disappear with the modules. Their successors are `org.sequeless.query.field.*` in sequeless-query (spec 14, section 4).

## 5. Modules and their dependencies

- **Module list after spec 16:** `ontology-core`, `schema-contract`, `behaviour-contract`, `automation-contract`, `ontology-jena-adapter`, `ontology-testfixtures`.
- **Root POM:** the `<modules>` list loses `facet-core` and `facet-core-testfixtures`. The root stays a parent-less aggregator.
- **Rules that go with the modules:** the zero-dependency Enforcer rule and the ArchUnit rule that guarded `facet-core` (ADR 0005, Consequences). No other module's rules change.

## 6. Release config (first version, tags, BOM change)

- **First version and tags:** none. The facet modules were never released. They have no tag (`git tag -l '*facet*'` is empty), no entry in `.release-please-manifest.json`, and no component in `release-please-config.json`. The testfixtures POM and the old README both said "frozen and not published from this repository".
- **`release-please-config.json`:** unchanged.
- **BOM change:** none. `sequeless-build`'s `sequeless-bom` (0.2.0 on `origin/main`) lists no facet artifact, so there is nothing to remove. This spec does not touch `sequeless-build`.
- **Release of this PR:** the deletion is a `chore` commit. Release-please has no facet component, so it adds no version bump.
- **Ordering:** the open release PR #25 (`ontology-jena-adapter` 0.2.1 to 0.3.0) merges before this PR, so the next release PR is generated from a `main` that already contains this change.

## 7. CI and workflows

- `ci.yml` and `codeql.yml` have no facet references and are unchanged. The gate is still `make verify`, now over the reduced reactor.
- `sonar-project.properties` drops the two facet JaCoCo paths.
- `release.yml` is unchanged.

## 8. ADRs

- **ADR 0005, "facet-core moves to sequeless-query":** Proposed to Accepted in this PR, with an `Accepted:` line that names this spec and spec 14. Spec 14's ADR 0001 cites it as "accepted by this move".
- **ADR 0002, "facet-core is a zero-dependency contract module":** superseded by ADR 0005, as ADR 0005's header states. Its text is left unchanged, following the precedent of ADR 0001 under ADR 0006.
- **ADR 0004, "thin core, Jena adapter, independent versions":** still mentions `facet-core-testfixtures` as the example of a testfixtures kit. Historical, so unchanged.
- **New ADR:** none. ADR 0005 already records the consequences, and this spec records the mechanics.

## 9. Definition of done and hazards

Definition of done (sequeless-prototype#44):

- **The new repo's CI is green.** This repo is not new. Its CI on this PR is the gate for merging (`build / maven`, `analyze / analyze (java-kotlin)`, `SonarCloud Code Analysis`). Result recorded in the handoff.
- **The prototype's CI is green against released artifacts.** Checked read-only in a throwaway checkout, with no prototype PR (see the handoff for the result). The prototype has no reference to facet-core, so the deletion cannot change what it resolves.
- **The BOM's CI, which runs every testkit contract, is green.** No BOM change. The `sequeless-build` BOM CI was checked read-only; its result is in the handoff.
- **The release and the BOM re-release are done.** No facet release exists, so none is needed. The ontology release that ships with this change is `ontology-jena-adapter` 0.3.0 (PR #25). A BOM re-release is not needed for this spec, because the BOM's content does not change. The BOM still pins `ontology-jena-adapter` 0.1.2, which is a separate follow-up.
- **Cut-over clauses:** not applicable. Facet-core was never in the prototype, so there is no prototype deletion to name.

Hazards:

- **Prototype and new repos differ in behaviour until spec 17.** The prototype keeps its own Browse and Read code until spec 17 consumes sequeless-query. This deletion does not change prototype behaviour. Until spec 17, the Field contract in sequeless-query and the prototype's own query code can diverge.
- **Prototype build risk from the deletion:** none found. The prototype has no facet-core reference in code or POM, and its BOM import lists no facet artifact.
- **Remote branch `docs/facet-javadoc-errors`** (`af119ba`, no open PR) edits facet-core's Javadoc. This PR deletes those files, so the branch cannot merge as it stands. It is not touched here. Deleting it is an owner action.
- **Stale sonar path strings** in sequeless-kernel, sequeless-automation and sequeless-template. Config only, not built, so they are not blocking. Follow-up, one cleanup per repo.
- **Sequeless-template comment:** `BoundaryRulesTest` names facet-core in a comment about the fourth boundary rule. Comment only, follow-up.
- **Release PR #25 merged first.** Any later release PR must be re-checked to make sure it has no facet component.
