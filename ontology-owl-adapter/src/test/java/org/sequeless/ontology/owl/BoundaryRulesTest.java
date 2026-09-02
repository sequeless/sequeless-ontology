package org.sequeless.ontology.owl;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Mechanically enforced hexagonal boundaries for {@code ontology-owl-adapter}, including its
 * cross-module relationship with {@code ontology-core}.
 */
@AnalyzeClasses(
        packages = "org.sequeless.ontology",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class BoundaryRulesTest {

    // TODO: drop allowEmptyShould once real types land in these packages — the scaffold has none yet.
    @ArchTest
    static final ArchRule api_and_spi_types_are_public = classes()
            .that()
            .resideInAnyPackage("org.sequeless.ontology.owl.api..", "org.sequeless.ontology.owl.spi..")
            .and()
            .areTopLevelClasses()
            .and()
            .haveSimpleNameNotEndingWith("package-info")
            .should()
            .bePublic()
            .as("the published contract (owl.api + owl.spi) must be exported as public types")
            .allowEmptyShould(true);

    // TODO: drop allowEmptyShould once real types land in these packages — the scaffold has none yet.
    @ArchTest
    static final ArchRule owl_adapter_does_not_reach_ontology_core_internals = noClasses()
            .that()
            .resideInAnyPackage("org.sequeless.ontology.owl..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.sequeless.ontology.internal..", "org.sequeless.ontology.spi..")
            .as("ontology-owl-adapter's main scope may only depend on ontology-core's api package")
            .allowEmptyShould(true);

    // TODO: drop allowEmptyShould once real types land in these packages — the scaffold has none yet.
    @ArchTest
    static final ArchRule ontology_core_does_not_depend_on_owl_adapter = noClasses()
            .that()
            .resideInAnyPackage("org.sequeless.ontology..")
            .and()
            .resideOutsideOfPackage("org.sequeless.ontology.owl..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.sequeless.ontology.owl..")
            .as("ontology-core must not depend on ontology-owl-adapter (documents the intended "
                    + "dependency direction; Maven's reactor build order already rejects this cycle)")
            .allowEmptyShould(true);
}
