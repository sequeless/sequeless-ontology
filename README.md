# sequeless-ontology

**sequeless-ontology** is the domain ontology model for Sequeless: a core library publishing
the ontology's `api` and `spi` contracts, plus a reference adapter for translating that model
to and from OWL. The facet contract that once lived here moved to
[`sequeless-query`](https://github.com/sequeless/sequeless-query) ([ADR 0005](docs/adr/0005-facet-core-moves-to-sequeless-query.md)).

## Add it as a dependency

Requires JDK 25.

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
| [`ontology-postgres-adapter`](ontology-postgres-adapter) | `org.sequeless:ontology-postgres-adapter` | PostgreSQL `OntologyDocumentStore` over `sq_ontology_document`, on plain JDBC. Ships no migration; its schema comes from `store-postgres-jsonb-adapter`. |

The repository root is a `pom`-packaged, parent-less aggregator (`org.sequeless:sequeless-ontology`)
that ships no code. It holds no dependency management, and its only build plugin sorts the root
`pom.xml`. Each module takes `org.sequeless:sequeless-build-parent:0.1.3` as its parent, which
supplies the shared build plugins and enforcer settings. Each released module
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

Architecture decisions live under [`docs/adr/`](docs/adr/). Extraction specs live under
[`docs/extraction/`](docs/extraction/).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[Apache License 2.0](LICENSE).
