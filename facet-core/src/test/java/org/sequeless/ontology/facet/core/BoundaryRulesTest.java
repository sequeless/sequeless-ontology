package org.sequeless.ontology.facet.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Mechanically enforced hexagonal boundaries and the zero-dependency rule for {@code facet-core}.
 */
@AnalyzeClasses(
        packages = "org.sequeless.ontology.facet.core",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class BoundaryRulesTest {

    @ArchTest
    static final ArchRule facet_core_is_free_of_spring = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .as("facet-core must stay a zero-dependency contract — no Spring dependency");

    @ArchTest
    static final ArchRule api_and_spi_types_are_public = classes()
            .that()
            .resideInAnyPackage("org.sequeless.ontology.facet.core.api..", "org.sequeless.ontology.facet.core.spi..")
            .and()
            .areTopLevelClasses()
            .and()
            .haveSimpleNameNotEndingWith("package-info")
            .should()
            .bePublic()
            .as("the published contract (api + spi) must be exported as public types");

    @ArchTest
    static final ArchRule api_and_spi_never_depend_on_internal = noClasses()
            .that()
            .resideInAnyPackage("org.sequeless.ontology.facet.core.api..", "org.sequeless.ontology.facet.core.spi..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.sequeless.ontology.facet.core.internal..")
            .as("api and spi must not depend on internal — internal is a pure-function implementation "
                    + "detail, not published surface");

    @ArchTest
    static final ArchRule module_depends_on_nothing_outside_java_and_itself = noClasses()
            .should()
            .dependOnClassesThat()
            .resideOutsideOfPackages("java..", "org.sequeless.ontology.facet.core..")
            .as("facet-core is a zero-dependency contract; this is the bytecode-level backstop for the "
                    + "Enforcer ban-all-runtime-dependencies rule (see facet-core/pom.xml)");
}
