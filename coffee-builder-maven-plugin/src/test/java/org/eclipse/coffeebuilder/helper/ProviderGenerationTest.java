package org.eclipse.coffeebuilder.helper;

import jakarta.json.Json;
import org.eclipse.coffeebuilder.helper.datasource.DataSourceClassCreator;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ProviderGenerationTest {

    @TempDir
    Path projectDirectory;

    @Test
    void classDatasourceRendersAdditionalProperties() throws Exception {
        var project = project();
        var parameters = Json.createObjectBuilder()
            .add("name", "java:app/jdbc/orders")
            .add("className", "org.h2.jdbcx.JdbcDataSource")
            .add("properties", Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("name", "MODE").add("value", "LEGACY")))
            .build();

        new DataSourceClassCreator(project, mock(Log.class))
            .dataSourceParameters(parameters)
            .build();

        var generated = Files.readString(providerPath("DataSourceProvider"));
        assertTrue(generated.contains("@DataSourceDefinition("));
        assertTrue(generated.contains("\"MODE=LEGACY\""));
    }

    @Test
    void persistenceProviderUsesConfiguredUnitName() throws Exception {
        var project = project();

        JakartaEeHelper.getInstance().addPersistenceClassProvider(
            project, mock(Log.class), "orders-unit");

        var generated = Files.readString(providerPath("PersistenceProvider"));
        assertTrue(generated.contains("unitName = \"orders-unit\""));
        assertTrue(!generated.contains("example-pu"));
    }

    private MavenProject project() {
        var model = new Model();
        model.setGroupId("com.example");
        model.setArtifactId("orders");
        var project = new MavenProject(model);
        project.setFile(projectDirectory.resolve("pom.xml").toFile());
        return project;
    }

    private Path providerPath(String className) {
        return projectDirectory.resolve("src/main/java/com/example/orders/infrastructure/provider/")
            .resolve(className + ".java");
    }
}
