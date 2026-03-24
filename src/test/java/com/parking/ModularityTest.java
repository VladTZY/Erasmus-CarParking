package com.parking;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

    private final ApplicationModules modules =
            ApplicationModules.of(ParkingApplication.class);

    /**
     * Verifies that no module imports another module's internal types.
     * This test will fail the build the moment a module boundary is violated.
     */
    @Test
    void verifyModuleBoundaries() {
        modules.verify();
    }

    /**
     * Prints a human-readable overview of all detected modules and their
     * dependencies to stdout. Useful during development to understand the
     * current module graph without opening a browser.
     */
    @Test
    void printModuleOverview() {
        modules.forEach(System.out::println);
    }

    /**
     * Generates AsciiDoc + PlantUML component diagrams into
     * target/spring-modulith-docs/. Open the .puml files in any PlantUML
     * viewer or paste them into https://www.plantuml.com/plantuml to visualise
     * the module graph.
     */
    @Test
    void writeDocumentationSnippets() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
