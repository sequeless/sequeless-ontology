# sequeless-ontology

**sequeless-ontology** is the domain ontology model for Sequeless: a core library publishing
the ontology's `api` and `spi` contracts, plus a reference adapter for translating that model
to and from OWL. This repository is currently a build scaffold — the module and package
boundaries are in place, but no domain types have landed yet.

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
| [`ontology-owl-adapter`](ontology-owl-adapter) | `org.sequeless:ontology-owl-adapter` | Reference adapter translating the ontology domain model to and from OWL, depending only on `ontology-core`'s published `api`. Scaffolded; no adapter types yet. |

The repository root is a `pom`-packaged aggregator/parent (`org.sequeless:sequeless-ontology`)
that ships no code — it holds the single shared version, dependency management, and build
plugins. All modules are released together from that one version.

## Build

```bash
make verify    # full build + tests + quality gates (Spotless, Enforcer, ArchUnit)
make test      # fast unit tests only
make fmt       # apply formatting (Spotless / Palantir Java Format)
```

`make verify` wraps `./mvnw -B verify`; the Maven wrapper is committed, so no local Maven
install is required.

## Documentation

Architecture decisions live under [`docs/adr/`](docs/adr/).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[Apache License 2.0](LICENSE).
