package org.sequeless.ontology.jena;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Mechanically enforced hexagonal boundaries for {@code ontology-jena-adapter}, including its
 * cross-module relationship with {@code ontology-core}.
 */
@AnalyzeClasses(
        packages = "org.sequeless.ontology",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class BoundaryRulesTest {

    @ArchTest
    static final ArchRule ontology_jena_adapter_does_not_reach_ontology_core_internals = noClasses()
            .that()
            .resideInAPackage("org.sequeless.ontology.jena..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.sequeless.ontology.internal..")
            .as("ontology-jena-adapter may use ontology-core's api and spi, but not its internal package");

    @ArchTest
    static final ArchRule ontology_core_does_not_depend_on_jena_adapter = noClasses()
            .that()
            .resideInAnyPackage("org.sequeless.ontology..")
            .and()
            .resideOutsideOfPackage("org.sequeless.ontology.jena..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.sequeless.ontology.jena..")
            .as("ontology-core must not depend on ontology-jena-adapter (documents the intended "
                    + "dependency direction; Maven's reactor build order already rejects this cycle)");
}
