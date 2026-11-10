package org.sequeless.ontology.testfixtures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Mechanically enforced hexagonal boundaries for {@code ontology-testfixtures}.
 */
@AnalyzeClasses(
        packages = "org.sequeless.ontology",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class BoundaryRulesTest {

    @ArchTest
    static final ArchRule contract_runners_stay_adapter_neutral = noClasses()
            .that()
            .resideInAPackage("org.sequeless.ontology.testfixtures..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.apache.jena..", "org.sequeless.ontology.jena..")
            .as("the contract runners validate any port implementation, so they must not depend on a concrete adapter");
}
