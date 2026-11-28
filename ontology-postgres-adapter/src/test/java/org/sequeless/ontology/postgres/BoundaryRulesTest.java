package org.sequeless.ontology.postgres;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Mechanically enforced boundary for {@code ontology-postgres-adapter}: production classes stay Spring-free.
 */
@AnalyzeClasses(
        packages = "org.sequeless.ontology.postgres",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class BoundaryRulesTest {

    @ArchTest
    static final ArchRule ontology_postgres_is_free_of_spring = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .as("sequeless-ontology must stay transport-agnostic — no Spring dependency");
}
