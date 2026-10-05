package org.eclipse.coffeebuilder.util;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.RegexValidator;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

import static org.eclipse.coffeebuilder.util.Constants.DATASOURCE_DECLARE_CLASS;
import static org.eclipse.coffeebuilder.util.Constants.DATASOURCE_DECLARE_WEB;

/**
 * Utility class for constructing and validating JNDI data source names.
 *
 * <p>This class provides methods to:
 * <ul>
 *   <li>Compute the appropriate JNDI prefix for a data source based on its declaration scope
 *       (web or class level).</li>
 *   <li>Validate a data source name against a strict regular expression and return the fully
 *       qualified JNDI name (prefix + jdbc/ + name).</li>
 * </ul>
 *
 * <p>Example:
 * <pre>
 * String jndi = DataSourceUtil.validateDataSourceName(DATASOURCE_DECLARE_WEB, "myDataSource");
 * </pre>
 *
 * <p>Validation rules: the data source name must start with a letter and contain only letters,
 * digits and underscores.</p>
 */
public class DataSourceUtil {

    private DataSourceUtil(){

    }
    /**
     * Returns the appropriate prefix for a data source name based on the declaration type. This prefix is used to construct the full JNDI name for the data source.
     *
     * @param declare The declaration type (e.g., DATASOURCE_DECLARE_WEB, DATASOURCE_DECLARE_CLASS).
     * @return The prefix for the data source name.
     */
    public static String getPrefix(String declare) {
        return switch (declare) {
            case DATASOURCE_DECLARE_WEB -> "java:global/";
            case DATASOURCE_DECLARE_CLASS -> "java:app/";
            default -> StringUtils.EMPTY;
        } + "jdbc/";
    }

    /**
     * Validates the given data source name against a regular expression and prepends the appropriate JNDI prefix based on the declaration type.
     *
     * @param declare        The declaration type (e.g., DATASOURCE_DECLARE_WEB, DATASOURCE_DECLARE_CLASS).
     * @param datasourceName The name of the data source to validate.
     * @return The fully qualified data source name with the prefix.
     * @throws IllegalArgumentException If the data source name is invalid.
     */
    public static String validateDataSourceName(String declare, String datasourceName) {
        RegexValidator validator = new RegexValidator("^[a-zA-Z][a-zA-Z0-9_]*$");
        if (!validator.isValid(datasourceName)) {
            throw new IllegalArgumentException("Invalid datasource name");
        }
        return getPrefix(declare) + datasourceName;
    }

    /**
     * Adds catalog-defined URL parameters that are not already supplied by the user.
     * Existing parameter names are matched case-insensitively. URLs that already use
     * query-string parameters retain that syntax; all other JDBC URLs use semicolon
     * parameters.
     *
     * @param jdbcUrl the user-provided JDBC URL
     * @param defaults catalog-defined default URL parameters
     * @return the URL with missing defaults appended
     */
    public static String mergeDefaultUrlParameters(String jdbcUrl, JsonObject defaults) {
        if (StringUtils.isBlank(jdbcUrl) || defaults == null || defaults.isEmpty()) {
            return jdbcUrl;
        }

        Set<String> existingNames = parameterNames(jdbcUrl);
        var missingParameters = new StringJoiner(jdbcUrl.contains("?") ? "&" : ";");
        defaults.forEach((name, value) -> {
            if (!existingNames.contains(normalize(name))) {
                missingParameters.add(name + "=" + parameterValue(value));
            }
        });
        if (missingParameters.length() == 0) {
            return jdbcUrl;
        }

        int fragmentIndex = jdbcUrl.indexOf('#');
        String baseUrl = fragmentIndex >= 0 ? jdbcUrl.substring(0, fragmentIndex) : jdbcUrl;
        String fragment = fragmentIndex >= 0 ? jdbcUrl.substring(fragmentIndex) : "";
        boolean queryStyle = baseUrl.contains("?");
        String separator;
        if (queryStyle) {
            separator = baseUrl.endsWith("?") || baseUrl.endsWith("&") ? "" : "&";
        } else {
            separator = baseUrl.endsWith(";") ? "" : ";";
        }
        return baseUrl + separator + missingParameters + fragment;
    }

    private static Set<String> parameterNames(String jdbcUrl) {
        Set<String> names = new HashSet<>();
        int semicolonIndex = jdbcUrl.indexOf(';');
        if (semicolonIndex >= 0) {
            int end = jdbcUrl.indexOf('?', semicolonIndex);
            addParameterNames(names, jdbcUrl.substring(semicolonIndex + 1,
                end >= 0 ? end : jdbcUrl.length()), ";");
        }
        int queryIndex = jdbcUrl.indexOf('?');
        if (queryIndex >= 0) {
            int end = jdbcUrl.indexOf('#', queryIndex);
            addParameterNames(names, jdbcUrl.substring(queryIndex + 1,
                end >= 0 ? end : jdbcUrl.length()), "[&;]");
        }
        return names;
    }

    private static void addParameterNames(Set<String> names, String parameters, String separator) {
        for (String parameter : parameters.split(separator)) {
            String name = StringUtils.substringBefore(parameter, "=").trim();
            if (StringUtils.isNotEmpty(name)) {
                names.add(normalize(name));
            }
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static String parameterValue(JsonValue value) {
        return value.getValueType() == JsonValue.ValueType.STRING
            ? ((jakarta.json.JsonString) value).getString()
            : value.toString();
    }

}
