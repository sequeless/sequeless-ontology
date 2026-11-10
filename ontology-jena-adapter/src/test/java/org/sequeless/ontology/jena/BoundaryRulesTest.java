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
    static final ArchRule ontology_jena_adapter_depends_only_on_allowed_packages = noClasses()
            .that()
            .resideInAPackage("org.sequeless.ontology.jena..")
            .should()
            .dependOnClassesThat()
            .resideOutsideOfPackages(
                    "java..",
                    "org.sequeless.ontology.api..",
                    "org.sequeless.ontology.spi..",
                    "org.sequeless.ontology.schema..",
                    "org.sequeless.ontology.behaviour..",
                    "org.sequeless.ontology.automation..",
                    "org.sequeless.ontology.jena..",
                    "org.apache.jena..",
                    "com.fasterxml.jackson..",
                    "org.sequeless.kernel..",
                    "org.sequeless.filter..")
            .as("ontology-jena-adapter may depend only on the ontology api, spi, schema, behaviour, automation "
                    + "and jena packages, the JDK and its third-party libraries (spec 11, section 5.4)");

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
