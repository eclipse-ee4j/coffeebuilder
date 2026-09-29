package org.eclipse.coffeebuilder.util;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CoffeeBuilderUtilTest {

    @Mock
    private Log mockLog;

    @AfterEach
    void clearDevelopmentMode() {
        System.clearProperty("devel");
    }

    @Test
    @DisplayName("public configuration API remains compatible with packaged configuration")
    void publicConfigurationApiLoadsPackagedConfiguration() throws Exception {
        System.clearProperty("devel");

        assertEquals("org.apache.maven.plugins", CoffeeBuilderUtil
            .getDependencyConfiguration(mockLog, "maven-compiler-plugin")
            .orElseThrow().getString("groupId"));
        assertEquals("7.2026.6", CoffeeBuilderUtil.getServerDefinition(mockLog, "payara")
            .orElseThrow().getString("11.0.0"));
        assertTrue(CoffeeBuilderUtil.getSpecificationsDefinitions(mockLog)
            .orElseThrow().containsKey("11.0.0"));
        assertEquals("java.util.UUID", CoffeeBuilderUtil.getClassesDefinitions(mockLog)
            .orElseThrow().getJsonObject("UUID").getString("fullName"));
        assertEquals("jaxrs-spec", CoffeeBuilderUtil.getOpenApiGeneratorConfiguration(mockLog)
            .orElseThrow().getString("generatorName"));
        assertEquals("hibernate.show_sql", CoffeeBuilderUtil
            .getPropertiesConfiguration(mockLog, "jpa-hibernate")
            .orElseThrow().getJsonObject(3).getString("name"));
        assertTrue(CoffeeBuilderUtil.getDialectConfiguration(mockLog)
            .orElseThrow().containsKey("postgresql"));
        assertEquals("6.1", CoffeeBuilderUtil.getSchema(mockLog, "11.0.0", "web-app")
            .orElseThrow().getString("version"));
        assertEquals("com.h2database:h2", CoffeeBuilderUtil
            .getJdbcConfiguration(mockLog, "jdbc:h2:mem:test")
            .orElseThrow().getString("coordinates"));
    }
}
