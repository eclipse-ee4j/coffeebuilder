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
package org.eclipse.coffeebuilder.helper.datasource;

import org.eclipse.coffeebuilder.util.MavenProjectUtil;
import org.eclipse.coffeebuilder.util.PathsUtil;
import org.eclipse.coffeebuilder.util.TemplateUtil;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.coffeebuilder.util.Constants.CLASS_NAME;
import static org.eclipse.coffeebuilder.util.Constants.NAME;
import static org.eclipse.coffeebuilder.util.Constants.PACKAGE_NAME;
import static org.eclipse.coffeebuilder.util.Constants.PROPERTIES;
import static org.eclipse.coffeebuilder.util.Constants.VALUE;

/**
 * Responsible for creating the DataSource class.
 * <p>
 * This class extends {@link DataSourceCreator} and provides the implementation
 * for building a DataSource provider class. It uses templates to generate the
 * Java class file with the appropriate annotations and properties.
 * </p>
 */
public class DataSourceClassCreator extends DataSourceCreator {

    /**
     * Constructs a new instance of {@code DataSourceClassCreator}.
     *
     * @param mavenProject the Maven project instance
     * @param log          the Maven plugin logger
     */
    public DataSourceClassCreator(MavenProject mavenProject, Log log) {
        super(mavenProject, log);
    }

    /**
     * Builds the DataSource provider class.
     * <p>
     * This method retrieves the package definition and class name, determines
     * the file path for the generated class, and uses a template to create the
     * Java class file. The generated class includes annotations for defining
     * the DataSource configuration.
     * </p>
     *
     * @throws IOException if an error occurs while creating the class file
     */
    @Override
    public void build() throws IOException {
        var packageDefinition = MavenProjectUtil.getProviderPackage(mavenProject);
        var className = "DataSourceProvider";
        var dataSourceClassPath = PathsUtil.getJavaPath(mavenProject, packageDefinition, className);
        var properties = new LinkedHashMap<>(getDataSourceParameters());
        if (properties.get(PROPERTIES) instanceof List<?> additionalProperties) {
            var annotationProperties = additionalProperties.stream()
                .map(Map.class::cast)
                .map(property -> "%s=%s".formatted(property.get(NAME), property.get(VALUE)))
                .toList();
            properties.put(PROPERTIES, annotationProperties);
        }
        var annotationClasses = Map.of(
            "jakarta.annotation.sql.DataSourceDefinition", properties
        );
        TemplateUtil.getInstance().createJavaBeanFile(log,
            Map.of(PACKAGE_NAME, packageDefinition,
                CLASS_NAME, className,
                "annotations", annotationClasses), dataSourceClassPath);
    }
}
