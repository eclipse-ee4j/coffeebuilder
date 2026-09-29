package org.eclipse.coffeebuilder.util;

import jakarta.json.Json;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.eclipse.coffeebuilder.util.Constants.DEPENDENCIES_URL;
import static org.eclipse.coffeebuilder.util.Constants.DEV_BASE_URL;
import static org.eclipse.coffeebuilder.util.Constants.SERVERS_URL;
import static org.eclipse.coffeebuilder.util.HttpUtil.STRING_TO_JSON_OBJECT_RESPONSE_CONVERTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class ConfigurationLoaderTest {

    private static final String[] CONFIGURATION_RESOURCES = {
        "classes-definitions.json",
        "dependencies.json",
        "hibernate-dialect.json",
        "openapi-generator-config.json",
        "properties.json",
        "schemas.json",
        "servers.json",
        "specifications.json"
    };

    @Mock
    private Log mockLog;

    @AfterEach
    void clearDevelopmentMode() {
        System.clearProperty("devel");
    }

    @Test
    @DisplayName("default mode loads configuration from the classpath")
    void defaultModeLoadsFromClasspath() throws Exception {
        System.clearProperty("devel");

        var dependencies = ConfigurationLoader.load(mockLog, DEPENDENCIES_URL);

        assertEquals("org.apache.maven.plugins",
            dependencies.getJsonObject("maven-compiler-plugin").getString("groupId"));
    }

    @Test
    @DisplayName("devel=false loads configuration from the classpath")
    void develFalseLoadsFromClasspath() throws Exception {
        System.setProperty("devel", "false");

        var servers = ConfigurationLoader.load(mockLog, SERVERS_URL);

        assertEquals("8.0.3", servers.getJsonObject("glassfish").getString("11.0.0"));
    }

    @Test
    @DisplayName("devel=true loads configuration from the development URL")
    void develTrueLoadsFromDevelopmentUrl() throws Exception {
        System.setProperty("devel", "true");
        var expected = Json.createObjectBuilder().add("source", "development").build();

        try (MockedStatic<HttpUtil> httpUtil = mockStatic(HttpUtil.class)) {
            httpUtil.when(() -> HttpUtil.getContent(mockLog, DEV_BASE_URL + DEPENDENCIES_URL,
                STRING_TO_JSON_OBJECT_RESPONSE_CONVERTER)).thenReturn(expected);

            var result = ConfigurationLoader.load(mockLog, DEPENDENCIES_URL);

            assertSame(expected, result);
            httpUtil.verify(() -> HttpUtil.getContent(mockLog, DEV_BASE_URL + DEPENDENCIES_URL,
                STRING_TO_JSON_OBJECT_RESPONSE_CONVERTER));
        }
    }

    @Test
    @DisplayName("all monorepo configuration JSON files are available on the plugin classpath")
    void packagedConfigurationResourcesAreAvailable() {
        for (var resource : CONFIGURATION_RESOURCES) {
            assertNotNull(ConfigurationLoader.class.getResource("/configuration/" + resource), resource);
        }
    }
}
