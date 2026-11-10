package org.sequeless.ontology.behaviour;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Mechanically enforced hexagonal boundaries for {@code behaviour-contract}.
 */
@AnalyzeClasses(
        packages = "org.sequeless.ontology",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class BoundaryRulesTest {

    /** ArchUnit places primitive and array types in the default package, so they are allowed explicitly. */
    private static final DescribedPredicate<JavaClass> PRIMITIVE_OR_ARRAY =
            DescribedPredicate.describe("is a primitive or array type", c -> c.isPrimitive() || c.isArray());

    @ArchTest
    static final ArchRule behaviour_depends_only_on_schema = classes()
            .that()
            .resideInAPackage("org.sequeless.ontology.behaviour..")
            .should()
            .onlyDependOnClassesThat(resideInAnyPackage(
                            "java..",
                            "org.sequeless.ontology.behaviour..",
                            "org.sequeless.ontology.schema..",
                            "org.sequeless.kernel..",
                            "org.sequeless.filter.api..")
                    .or(PRIMITIVE_OR_ARRAY))
            .as("the Behaviour half may depend on the Schema half, the kernel and the filter api, and nothing else");

    @ArchTest
    static final ArchRule api_and_spi_types_are_public = classes()
            .that()
            .resideInAnyPackage("org.sequeless.ontology.behaviour.api..", "org.sequeless.ontology.behaviour.spi..")
            .and()
            .areTopLevelClasses()
            .and()
            .haveSimpleNameNotEndingWith("package-info")
            .should()
            .bePublic()
            .as("the published contract (api + spi) must be exported as public types");
}
