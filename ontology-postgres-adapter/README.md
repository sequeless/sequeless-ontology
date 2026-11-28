# ontology-postgres-adapter

`org.sequeless:ontology-postgres-adapter`: a PostgreSQL `OntologyDocumentStore` over the `sq_ontology_document` table.

- **Compile dependency:** `ontology-core` only, on plain JDBC. No Spring (the module's enforcer bans `org.springframework*`).
- **No migration.** The table is created by `store-postgres-jsonb-adapter` (V1). This module's tests take that adapter at test scope, for its `PostgresMigrations` only.
- **Single version:** the store adapter brings `ontology-core` 0.1.0 transitively; the test dependency excludes it, and `banDuplicateClasses` in `pom.xml` fails the build on any duplicate class.

## Provenance

The class and its contract test were copied from the prototype and then changed. Nothing else was imported.

| | |
|---|---|
| Source repository | `sequeless/sequeless-prototype` |
| Source commit | `429b2de` |
| Source main | `sequeless-adapter-persistence-postgres/src/main/java/org/sequeless/store/postgres/PostgresOntologyDocumentStore.java` |
| Source test | `sequeless-adapter-persistence-postgres/src/test/java/org/sequeless/store/postgres/PostgresOntologyDocumentStoreContractIT.java` |
| Tracking | `sequeless/sequeless-prototype#46` (spec 18, Q13, U13) |

Changes from the source, each a decision by the Navigator (spec 18 Q-A, Q-B, Q-C):

1. **Plain JDBC, no Spring (Q-A).** `JdbcClient` becomes `PreparedStatement` on a `DataSource`. `TransactionTemplate` becomes one connection with auto-commit off, committed on success and rolled back on failure. The SQL, the tenant advisory lock and the microsecond truncation are unchanged.
2. **Package `org.sequeless.ontology.postgres` (Q-B).** The source package `org.sequeless.store.postgres` is also the package of the same class in `store-postgres-jsonb-adapter`, so a test classpath would carry two classes with one name.
3. **`public` class and constructor (Q-C).** The constructor takes a `DataSource`. The prototype's package-private form would not let the host consume it from another package.

The contract test keeps the source's test body. It takes the schema from `PostgresMigrations` in the store adapter, and it uses `PGSimpleDataSource` in place of Spring's `SimpleDriverDataSource`.
