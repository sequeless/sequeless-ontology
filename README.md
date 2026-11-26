# sequeless-ontology

A transition is a named change from one state to another state within a state machine.
A state machine is a set of states and transitions. An action is work that runs during a transition.
An ontology describes the types, properties, state machines, and actions of an application.
sequeless-ontology is a Java library. It defines the ontology model for Sequeless. Sequeless is a set of Java libraries for domain ontology models, published under the `org.sequeless` Maven group.
The library contains contracts. A contract is a set of Java interfaces and Java records.
An ontology document is a file that describes an ontology in OWL (Web Ontology Language). An adapter is a class that connects the contracts to a library. A reference adapter reads ontology documents.

## Requirements

1. Install JDK 25 (Java Development Kit version 25). Or open the devcontainer (a container configuration for development) in `.devcontainer/`. It contains JDK 25.
2. Maven is a Java build tool. Use the Maven wrapper, `./mvnw`, from the repository root. The wrapper runs Maven. A local Maven installation is not necessary.

## Modules

The root `pom.xml` file (the Maven build file) lists the modules below. The root `pom.xml` contains no Java code. Each module uses `org.sequeless:sequeless-build-parent:0.1.3` as its parent POM (Project Object Model), the shared Maven build file. Turtle is a text format for RDF (Resource Description Framework).

API means application programming interface. SPI means service provider interface. The `api` Java package holds the public Java classes and interfaces that callers use. The `spi` Java package holds the interfaces that other code implements.
A trigger is what causes a transition to start.
A derived property gets its value from other properties.
In the behaviour contract, a rule computes a derived property's value.

| Module | Maven coordinates (group:artifact:version) | Purpose |
| --- | --- | --- |
| `ontology-core` | `org.sequeless:ontology-core:0.2.0` | Administration API, validation API, and storage SPI. |
| `schema-contract` | `org.sequeless:schema-contract:0.2.0` | Classes that describe object kinds and properties (the schema), and the `SchemaSource` SPI. |
| `behaviour-contract` | `org.sequeless:behaviour-contract:0.2.0` | Types that describe state machines, triggers, and rules, and the `BehaviourSource` SPI. |
| `automation-contract` | `org.sequeless:automation-contract:0.2.0` | Types that describe actions for transitions, and the `AutomationSource` SPI. |
| `ontology-jena-adapter` | `org.sequeless:ontology-jena-adapter:0.3.0` | Reference adapter that reads OWL with Apache Jena, a Java library for RDF data. |
| `ontology-testfixtures` | `org.sequeless:ontology-testfixtures:0.2.0` | Test kits that check SPI implementations, sample Turtle files, and SPI implementations that keep data in memory, for tests. |

The versions come from `.release-please-manifest.json`. release-please (a release automation tool) releases each module separately.

A module must not depend on Spring, a Java framework. The Enforcer plugin (a Maven dependency checker) blocks Spring dependencies.

Each module has a `BoundaryRulesTest` class. It uses ArchUnit (a library for architecture tests) to check the boundaries of the Java packages.

## Current Java classes and interfaces

`ontology-core` contains these public types:

- `api`: `OntologyAdministration`, `OntologyDocument`, `OntologyFormat`, `ImportMode`, `ImportReport`, `OntologyReport`, `OntologyIssue`, `Severity`, `OntologyException`, `ShapeValidationPort`, `ValidatableObject`, `ValidationSchema`, `ShapeViolation`.
- `spi`: `OntologyDocumentStore`, `StoredOntologyDocument`.

Start with `OntologyAdministration`. It has the operations to reload, export, and import ontology documents.

`ontology-jena-adapter` contains `JenaOntologyPort`, the reference adapter class. It implements `SchemaSource`, `BehaviourSource`, `AutomationSource`, and `OntologyAdministration`. `ShaclValidationPort` validates objects with SHACL (Shapes Constraint Language). It uses the SHACL shapes (validation constraints) in the loaded ontology.

## Commands

Run these commands from the repository root.

| Command | Effect |
| --- | --- |
| `make verify` | Runs `./mvnw -B verify`. Builds all modules, runs all tests, and runs the quality checks (Spotless and ArchUnit checks). |
| `make test` | Runs `./mvnw -B -DskipITs test`. Runs the fast unit tests only. |
| `make fmt` | Runs `./mvnw -q spotless:apply`. Formats the code with Spotless (a code formatting plugin). |
| `make new-adr title="Short title"` | Runs `scripts/new-adr.sh`. Creates the next numbered ADR (architecture decision record) in `docs/adr/`. |

Run `make help` to list all commands.

To submit a change:

1. Make a branch from `main`. A branch is a separate line of changes in Git (a version control tool).
2. Run `make fmt`.
3. Run `make verify`.
4. Save your change in Git.
5. Open a pull request. A pull request is a request to merge a branch into `main`.

To add a decision:

1. Run `make new-adr title="Short title"`.
2. Write the Context, Decision, and Consequences sections.

## Documentation

The `sq:` vocabulary is the set of terms that ontology documents use.

- `docs/adr/`: architecture decision records. Each record explains one decision. The template is `docs/adr/_template.md`.
- `docs/extraction/`: extraction specifications. They describe how code moved between Sequeless repositories.
- `ontology-jena-adapter/src/main/resources/ontology/`: Contains `sq-schema.ttl` and `sq-behaviour.ttl`.
- `ontology-jena-adapter/src/main/resources/ontology/`: These Turtle files define the `sq:` vocabulary.
- `ontology-testfixtures/src/main/resources/ontology/`: Turtle test files.
- `CHANGELOG.md`: Each module has a `CHANGELOG.md`. release-please writes these files. The root `CHANGELOG.md` holds older history and release-please does not write the root `CHANGELOG.md`.
- `release-please-config.json` and `.release-please-manifest.json`: release-please reads these files. The manifest holds the current version of each module.

Do not edit any `CHANGELOG.md` manually.

To change a decision, write a new ADR. Do not edit the old ADR.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before you open a pull request. It describes the contribution steps, commit format, and quality checks.

Continuous integration (CI) runs on pushes and pull requests to `main`. It calls the shared Maven workflow in `sequeless/sequeless-build`. CodeQL, a static analysis tool, runs on pull requests, on pushes to `main`, and every week. The release workflow (an automated process that uses release-please to release modules) runs on each push to `main`.

## Notes

The facet-core module moved to [`sequeless-query`](https://github.com/sequeless/sequeless-query). Read [ADR 0005](docs/adr/0005-facet-core-moves-to-sequeless-query.md) for the details.

## License

This project uses the [Apache License 2.0](LICENSE).
