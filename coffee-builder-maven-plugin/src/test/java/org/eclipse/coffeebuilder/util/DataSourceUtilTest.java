package org.eclipse.coffeebuilder.util;

import jakarta.json.Json;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.eclipse.coffeebuilder.util.Constants.DATASOURCE_DECLARE_ASADMIN;
import static org.eclipse.coffeebuilder.util.Constants.DATASOURCE_DECLARE_CLASS;
import static org.eclipse.coffeebuilder.util.Constants.DATASOURCE_DECLARE_WEB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataSourceUtilTest {

    @Nested
    @DisplayName("mergeDefaultUrlParameters")
    class MergeDefaultUrlParameters {

        @Test
        void addsH2LegacyModeAndPreservesExistingParameter() {
            var defaults = Json.createObjectBuilder().add("MODE", "LEGACY").build();

            assertEquals("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=LEGACY",
                DataSourceUtil.mergeDefaultUrlParameters(
                    "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", defaults));
        }

        @Test
        void preservesUserSuppliedH2ModeWithoutDuplication() {
            var defaults = Json.createObjectBuilder().add("MODE", "LEGACY").build();

            assertEquals("jdbc:h2:mem:test;mode=PostgreSQL;DB_CLOSE_DELAY=-1",
                DataSourceUtil.mergeDefaultUrlParameters(
                    "jdbc:h2:mem:test;mode=PostgreSQL;DB_CLOSE_DELAY=-1", defaults));
        }

        @Test
        void leavesUrlUnchangedWithoutDefaults() {
            assertEquals("jdbc:hsqldb:mem:test",
                DataSourceUtil.mergeDefaultUrlParameters(
                    "jdbc:hsqldb:mem:test", Json.createObjectBuilder().build()));
        }

        @Test
        void addsMissingQueryDefaultWithoutBreakingExistingParameter() {
            var defaults = Json.createObjectBuilder()
                .add("sslMode", "required")
                .add("connectTimeout", "30")
                .build();

            assertEquals("jdbc:example://localhost/test?connectTimeout=10&sslMode=required",
                DataSourceUtil.mergeDefaultUrlParameters(
                    "jdbc:example://localhost/test?connectTimeout=10", defaults));
        }

        @Test
        void userSuppliedQueryParameterOverridesDefaultCaseInsensitively() {
            var defaults = Json.createObjectBuilder().add("sslMode", "required").build();

            assertEquals("jdbc:example://localhost/test?SSLMODE=disabled",
                DataSourceUtil.mergeDefaultUrlParameters(
                    "jdbc:example://localhost/test?SSLMODE=disabled", defaults));
        }
    }

    @Nested
    @DisplayName("getPrefix")
    class GetPrefix {

        @Test
        @DisplayName("should return java:global/jdbc/ for web.xml declaration")
        void returnGlobalPrefixForWeb() {
            assertEquals("java:global/jdbc/", DataSourceUtil.getPrefix(DATASOURCE_DECLARE_WEB));
        }

        @Test
        @DisplayName("should return java:app/jdbc/ for class declaration")
        void returnAppPrefixForClass() {
            assertEquals("java:app/jdbc/", DataSourceUtil.getPrefix(DATASOURCE_DECLARE_CLASS));
        }

        @Test
        @DisplayName("should return jdbc/ for unknown declaration")
        void returnJdbcPrefixForUnknown() {
            assertEquals("jdbc/", DataSourceUtil.getPrefix("unknown"));
        }

        @Test
        @DisplayName("should return jdbc/ for asadmin declaration")
        void returnJdbcPrefixForAsadmin() {
            assertEquals("jdbc/", DataSourceUtil.getPrefix(DATASOURCE_DECLARE_ASADMIN));
        }
    }

    @Nested
    @DisplayName("validateDataSourceName")
    class ValidateDataSourceName {

        @Test
        @DisplayName("should return fully qualified name for valid datasource name")
        void returnsFullyQualifiedName() {
            String result = DataSourceUtil.validateDataSourceName(DATASOURCE_DECLARE_WEB, "myDataSource");
            assertEquals("java:global/jdbc/myDataSource", result);
        }

        @Test
        @DisplayName("should accept name starting with letter and containing digits/underscores")
        void acceptsValidNameWithDigitsAndUnderscores() {
            String result = DataSourceUtil.validateDataSourceName(DATASOURCE_DECLARE_CLASS, "db_01_main");
            assertEquals("java:app/jdbc/db_01_main", result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"1invalid", "my-ds", "my ds", "ds@name", ""})
        @DisplayName("should throw IllegalArgumentException for invalid names")
        void throwsForInvalidName(String invalidName) {
            assertThrows(IllegalArgumentException.class,
                () -> DataSourceUtil.validateDataSourceName(DATASOURCE_DECLARE_WEB, invalidName));
        }
    }
}
