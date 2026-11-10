# 0006. Supersede two clauses of 0001: versions per module and a thin ontology-core

- Status: proposed
- Date: 2026-11-10
- Supersedes: 0001 (in part)

## Context

ADR 0001 adopted a Maven reactor with an api/spi/internal hexagon. It also made two decisions that the ontology move does not fit. It released the whole reactor at one version, so a major change to the Behaviour contract would force a major change to the Schema contract, which the Query side depends on and nothing else does. And it pictured `ontology-core` as a library-neutral domain model, with a separate OWL adapter translating into and out of it. The ontology code moving in from the prototype has no such model. Its compiler maps the authored OWL and SHACL straight into the Schema and Behaviour contracts, which already are the library-neutral model.

## Decision

This ADR supersedes two clauses of ADR 0001, and only these:

1. **Versions.** Each module is versioned and released on its own. The single version for the whole reactor no longer applies.
2. **`ontology-core`.** It is thin. It holds the `OntologyAdministration` api, the validation api and the `OntologyDocumentStore` spi. It is not a library-neutral domain model, and the Jena adapter is `ontology-jena-adapter`, not an OWL adapter.

The api/spi/internal hexagon and the Maven reactor layout of ADR 0001 stay in force. ADR 0004 gives the details of the change.

## Consequences

- ADR 0001 is not edited, as ADR 0000 requires. Its status stays `accepted`. Read its single-version and domain-model clauses through this ADR.
- Only the two clauses above change. The other clauses of 0001 are still in force.
- This ADR and ADR 0004 share one status, `proposed`.
