package org.sequeless.ontology.schema;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Mechanically enforced hexagonal boundaries for {@code schema-contract}.
 */
@AnalyzeClasses(
        packages = "org.sequeless.ontology",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class BoundaryRulesTest {

    /** ArchUnit places primitive and array types in the default package, so they are allowed explicitly. */
    private static final DescribedPredicate<JavaClass> PRIMITIVE_OR_ARRAY =
            DescribedPredicate.describe("is a primitive or array type", c -> c.isPrimitive() || c.isArray());

    @ArchTest
    static final ArchRule schema_depends_only_on_jdk_kernel_and_itself = classes()
            .that()
            .resideInAPackage("org.sequeless.ontology.schema..")
            .should()
            .onlyDependOnClassesThat(
                    resideInAnyPackage("java..", "org.sequeless.ontology.schema..", "org.sequeless.kernel..")
                            .or(PRIMITIVE_OR_ARRAY))
            .as("the Schema half may depend on the JDK, the kernel and its own package, and nothing else");

    @ArchTest
    static final ArchRule api_and_spi_types_are_public = classes()
            .that()
            .resideInAnyPackage("org.sequeless.ontology.schema.api..", "org.sequeless.ontology.schema.spi..")
            .and()
            .areTopLevelClasses()
            .and()
            .haveSimpleNameNotEndingWith("package-info")
            .should()
            .bePublic()
            .as("the published contract (api + spi) must be exported as public types");
}
