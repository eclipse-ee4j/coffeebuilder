/*
 * Copyright 2024 Diego Silva diego.silva at apuntesdejava.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.coffeebuilder.util;

import jakarta.json.JsonObject;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.eclipse.coffeebuilder.util.Constants.DEV_BASE_URL;
import static org.eclipse.coffeebuilder.util.HttpUtil.STRING_TO_JSON_OBJECT_RESPONSE_CONVERTER;

/**
 * Loads Coffee Builder configuration from the plugin classpath or the development repository.
 */
final class ConfigurationLoader {

    private static final String CLASSPATH_BASE_PATH = "/configuration";

    private ConfigurationLoader() {
    }

    /**
     * Loads and parses a configuration JSON object.
     *
     * @param log               the Maven logger used for remote requests
     * @param configurationPath the configuration path, such as {@code /dependencies.json}
     * @return the parsed configuration
     * @throws IOException if the configuration cannot be read
     */
    static JsonObject load(Log log, String configurationPath) throws IOException {
        var normalizedPath = configurationPath.startsWith("/")
            ? configurationPath : "/" + configurationPath;

        if (BooleanUtils.toBoolean(System.getProperty("devel", "false"))) {
            return HttpUtil.getContent(log, DEV_BASE_URL + normalizedPath,
                STRING_TO_JSON_OBJECT_RESPONSE_CONVERTER);
        }

        return loadFromClasspath(normalizedPath);
    }

    private static JsonObject loadFromClasspath(String configurationPath) throws IOException {
        var resourcePath = CLASSPATH_BASE_PATH + configurationPath;

        try (var input = ConfigurationLoader.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("Configuration resource not found: " + resourcePath);
            }
            return STRING_TO_JSON_OBJECT_RESPONSE_CONVERTER.apply(
                new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
