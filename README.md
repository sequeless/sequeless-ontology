# sequeless-ontology

**sequeless-ontology** is the domain ontology model for Sequeless: a core library publishing
the ontology's `api` and `spi` contracts, plus a reference adapter for translating that model
to and from OWL. It also holds `facet-core`, a frozen zero-dependency contract by which a query
layer discovers what can be filtered in a compiled ontology. The facet modules are frozen and are
not published from this repository; they are the vocabulary this repository and
[`sequeless-filter`](https://github.com/sequeless/sequeless-filter) share so that neither depends
on the other.

## Add it as a dependency

Requires JDK 21.

```xml
<dependency>
  <groupId>org.sequeless</groupId>
  <artifactId>ontology-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Modules

| Module | Coordinates | Contents |
| --- | --- | --- |
| [`ontology-core`](ontology-core) | `org.sequeless:ontology-core` | The ontology domain core: published `api`/`spi` contracts and internal supporting types. Scaffolded; no domain types yet. |
| [`ontology-jena-adapter`](ontology-jena-adapter) | `org.sequeless:ontology-jena-adapter` | Reference adapter translating the ontology domain model to and from OWL, depending only on `ontology-core`'s published `api`. Scaffolded; no adapter types yet. |
| [`facet-core`](facet-core) | `org.sequeless:ontology-facet-core` | The facet contract shared with `sequeless-filter`: what can be filtered in a compiled ontology, its type, capabilities, and the versions defining it. Zero compile/runtime dependencies, enforced by the build. See [design.md](docs/specs/facet-contract/design.md) and [ADR 0002](docs/adr/0002-facet-core-is-a-zero-dependency-contract-module.md). |
| [`facet-core-testfixtures`](facet-core-testfixtures) | `org.sequeless:ontology-facet-core-testfixtures` | Implementation-neutral fixtures, an in-memory reference `FacetSource`/`ConceptSource`, and the reusable `FacetSourceContractTest`/`ConceptSourceContractTest` kit any adapter — including the RDF-backed one that lands later — validates itself against. Frozen and not published from this repository; carries JUnit/AssertJ at compile scope. |

The repository root is a `pom`-packaged aggregator and parent (`org.sequeless:sequeless-ontology`)
that ships no code. It holds dependency management and build plugins. Each released module
declares its own `groupId` and `version` in its POM and is released on its own by release-please
(see [`release-please-config.json`](release-please-config.json)).

## Build

```bash
make verify    # full build + tests + quality gates (Spotless, Enforcer, ArchUnit)
make test      # fast unit tests only
make fmt       # apply formatting (Spotless / Palantir Java Format)
```

`make verify` wraps `./mvnw -B verify`; the Maven wrapper is committed, so no local Maven
install is required.

## Documentation

Architecture decisions live under [`docs/adr/`](docs/adr/); normative specifications live under
[`docs/specs/`](docs/specs/). The facet contract's specification is
[`docs/specs/facet-contract/design.md`](docs/specs/facet-contract/design.md) — it is the source of
truth for `facet-core`, and is kept identical to its copy in `sequeless-filter`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[Apache License 2.0](LICENSE).
