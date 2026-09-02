package org.sequeless.ontology;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Mechanically enforced hexagonal boundaries for {@code ontology-core}.
 */
@AnalyzeClasses(
        packages = "org.sequeless.ontology",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class BoundaryRulesTest {

    // TODO: drop allowEmptyShould once real types land in these packages — the scaffold has none yet.
    @ArchTest
    static final ArchRule ontology_is_free_of_spring = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .as("sequeless-ontology must stay transport-agnostic — no Spring dependency")
            .allowEmptyShould(true);

    // TODO: drop allowEmptyShould once real types land in these packages — the scaffold has none yet.
    @ArchTest
    static final ArchRule api_and_spi_types_are_public = classes()
            .that()
            .resideInAnyPackage("org.sequeless.ontology.api..", "org.sequeless.ontology.spi..")
            .and()
            .areTopLevelClasses()
            .and()
            .haveSimpleNameNotEndingWith("package-info")
            .should()
            .bePublic()
            .as("the published contract (api + spi) must be exported as public types")
            .allowEmptyShould(true);
}
