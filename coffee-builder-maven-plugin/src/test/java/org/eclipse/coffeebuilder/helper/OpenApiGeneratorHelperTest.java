package org.eclipse.coffeebuilder.helper;

import jakarta.json.Json;
import org.eclipse.coffeebuilder.util.CoffeeBuilderUtil;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class OpenApiGeneratorHelperTest {

    @TempDir
    Path tempDirectory;

    @Test
    void openApiGeneratorRegistersItsOutputAsACompileSourceRoot() throws Exception {
        var projectDirectory = Files.createDirectory(tempDirectory.resolve("project"));
        var inputDirectory = Files.createDirectory(tempDirectory.resolve("input"));
        var specification = inputDirectory.resolve("openapi.yml");
        Files.writeString(specification, "openapi: 3.0.3\ninfo:\n  title: Test\n  version: 1.0.0\npaths: {}\n");

        var model = new Model();
        model.setGroupId("com.example");
        model.setArtifactId("orders");
        model.setBuild(new Build());
        var project = new MavenProject(model);
        project.setOriginalModel(model);
        project.setFile(projectDirectory.resolve("pom.xml").toFile());
        var log = mock(Log.class);
        var generatorConfig = Json.createObjectBuilder()
            .add("generatorName", "jaxrs-spec")
            .add("configOptions", Json.createObjectBuilder()
                .add("outputDir", "${project.build.directory}/generated-sources/openapi"))
            .build();
        var pluginDependency = Json.createObjectBuilder().add("version", "7.23.0").build();

        try (MockedStatic<CoffeeBuilderUtil> coffeeBuilder = mockStatic(CoffeeBuilderUtil.class)) {
            coffeeBuilder.when(() -> CoffeeBuilderUtil.getOpenApiGeneratorConfiguration(log))
                .thenReturn(Optional.of(generatorConfig));
            coffeeBuilder.when(() -> CoffeeBuilderUtil.getDependencyConfiguration(
                    log, "openapi-generator-maven-plugin"))
                .thenReturn(Optional.of(pluginDependency));

            OpenApiGeneratorHelper.getInstance().processServer(project, specification.toFile(), log);
        }

        var plugin = model.getBuild().getPlugins().stream()
            .filter(candidate -> "openapi-generator-maven-plugin".equals(candidate.getArtifactId()))
            .findFirst()
            .orElseThrow();
        var executionConfiguration = (Xpp3Dom) plugin.getExecutions().getFirst().getConfiguration();
        assertEquals("true", executionConfiguration.getChild("addCompileSourceRoot").getValue());
        assertEquals("${project.build.directory}/generated-sources/openapi",
            executionConfiguration.getChild("configOptions").getChild("outputDir").getValue());
        assertNull(model.getBuild().getPluginsAsMap().get(
            "org.codehaus.mojo:build-helper-maven-plugin"));
    }
}
