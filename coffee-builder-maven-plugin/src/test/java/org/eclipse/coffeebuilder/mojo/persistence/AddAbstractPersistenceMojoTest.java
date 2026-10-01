package org.eclipse.coffeebuilder.mojo.persistence;

import jakarta.json.JsonObject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddAbstractPersistenceMojoTest {

    @Test
    void parsesCommaSeparatedNameColonValueProperties() {
        var mojo = new TestPersistenceMojo();
        mojo.datasourceName = "testDatasource";
        mojo.declare = "web.xml";
        mojo.properties = "ssl:true,schema:inventory";

        JsonObject parameters = mojo.dataSourceParameters();

        var properties = parameters.getJsonArray("properties");
        assertEquals(2, properties.size());
        assertEquals("ssl", properties.getJsonObject(0).getString("name"));
        assertEquals("true", properties.getJsonObject(0).getString("value"));
        assertEquals("schema", properties.getJsonObject(1).getString("name"));
        assertEquals("inventory", properties.getJsonObject(1).getString("value"));
    }

    private static final class TestPersistenceMojo extends AddAbstractPersistenceMojo {

        JsonObject dataSourceParameters() {
            return getDataSourceParameters();
        }

        @Override
        public void execute() throws MojoExecutionException, MojoFailureException {
            // Not used by this parser contract test.
        }
    }
}
