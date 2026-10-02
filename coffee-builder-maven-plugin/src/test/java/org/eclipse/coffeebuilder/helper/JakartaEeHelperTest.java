package org.eclipse.coffeebuilder.helper;

import jakarta.json.Json;
import org.eclipse.coffeebuilder.util.CoffeeBuilderUtil;
import org.eclipse.coffeebuilder.util.PomUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class JakartaEeHelperTest {

    @Test
    void checkDataDependenciesAddsVersionedJdbcDriverWithoutCdiDependency() throws Exception {
        var project = mock(MavenProject.class);
        var log = mock(Log.class);
        var definition = Json.createObjectBuilder()
            .add("coordinates", "com.h2database:h2")
            .add("version", "2.4.240")
            .build();

        try (MockedStatic<PomUtil> pomUtil = mockStatic(PomUtil.class)) {
            pomUtil.when(() -> PomUtil.getDependency(project, log,
                    "jakarta.enterprise", "jakarta.enterprise.cdi-api"))
                .thenReturn(Optional.empty());

            JakartaEeHelper.getInstance().checkDataDependencies(project, log, definition);

            pomUtil.verify(() -> PomUtil.addDependency(project, log,
                "com.h2database:h2:2.4.240"));
        }
    }

    @Test
    void checkDataDependenciesPropagatesJdbcResolutionFailure() throws Exception {
        var project = mock(MavenProject.class);
        var log = mock(Log.class);
        var definition = Json.createObjectBuilder()
            .add("coordinates", "org.postgresql:postgresql")
            .build();
        var resolutionFailure = new MojoExecutionException("TLS metadata failure");

        try (MockedStatic<PomUtil> pomUtil = mockStatic(PomUtil.class)) {
            pomUtil.when(() -> PomUtil.getDependency(project, log,
                    "jakarta.enterprise", "jakarta.enterprise.cdi-api"))
                .thenReturn(Optional.empty());
            pomUtil.when(() -> PomUtil.addDependency(project, log,
                    "org.postgresql:postgresql"))
                .thenThrow(resolutionFailure);

            var thrown = assertThrows(MojoExecutionException.class,
                () -> JakartaEeHelper.getInstance().checkDataDependencies(project, log, definition));

            assertSame(resolutionFailure, thrown);
        }
    }

    @Test
    void addPrimeFacesUsesEmbeddedVersion() throws Exception {
        var project = mock(MavenProject.class);
        var log = mock(Log.class);
        var dependency = Json.createObjectBuilder()
            .add("groupId", "org.primefaces")
            .add("artifactId", "primefaces")
            .add("version", "15.0.17")
            .build();

        try (MockedStatic<CoffeeBuilderUtil> coffeeBuilder = mockStatic(CoffeeBuilderUtil.class);
             MockedStatic<PomUtil> pomUtil = mockStatic(PomUtil.class)) {
            coffeeBuilder.when(() -> CoffeeBuilderUtil.getDependencyConfiguration(log, "primefaces"))
                .thenReturn(Optional.of(dependency));

            JakartaEeHelper.getInstance().addPrimeFacesDependency(project, log);

            pomUtil.verify(() -> PomUtil.addDependency(project, log,
                "org.primefaces", "primefaces", "15.0.17", null, "jakarta", java.util.List.of()));
        }
    }

    @Test
    void addPrimeFacesPropagatesConfigurationLoadFailure() throws Exception {
        var project = mock(MavenProject.class);
        var log = mock(Log.class);
        var failure = new IOException("configuration unavailable");

        try (MockedStatic<CoffeeBuilderUtil> coffeeBuilder = mockStatic(CoffeeBuilderUtil.class)) {
            coffeeBuilder.when(() -> CoffeeBuilderUtil.getDependencyConfiguration(log, "primefaces"))
                .thenThrow(failure);

            var thrown = assertThrows(IOException.class,
                () -> JakartaEeHelper.getInstance().addPrimeFacesDependency(project, log));

            assertSame(failure, thrown);
        }
    }
}
