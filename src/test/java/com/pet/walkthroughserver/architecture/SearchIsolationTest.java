package com.pet.walkthroughserver.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class SearchIsolationTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.pet.walkthroughserver");
    }

    @Test
    void walkthroughModuleMustNotDependOnElasticsearchClient() {
        noClasses().that().resideInAPackage("..modules.walkthrough..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "co.elastic.clients..",
                        "org.springframework.data.elasticsearch..",
                        "org.springframework.amqp.."
                ).check(classes);
    }

    @Test
    void elasticsearchClientOnlyInSearchInfra() {
        noClasses().that().resideOutsideOfPackage("..modules.search.infra..")
                .should().dependOnClassesThat().resideInAPackage("co.elastic.clients..")
                .check(classes);
    }

    @Test
    void searchPresentationShouldNotDependOnElasticsearchClient() {
        noClasses().that().resideInAPackage("..modules.search.presentation..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "co.elastic.clients.."
                ).check(classes);
    }

    @Test
    void searchBusinessShouldNotDependOnElasticsearchClient() {
        noClasses().that().resideInAPackage("..modules.search.business..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "co.elastic.clients.."
                ).check(classes);
    }
}
