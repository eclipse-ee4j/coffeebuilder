/*
 * Copyright 2026 Diego Silva diego.silva at apuntesdejava.com.
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
package org.eclipse.coffeebuilder.helper;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.coffeebuilder.util.TemplateUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ManagedBeanCrudTemplateTest {

    @TempDir
    Path tempDirectory;

    @Test
    void bulkDeleteLeavesSelectionValidAndSubsequentIndividualDeleteIsNullSafe() throws Exception {
        Path generatedBean = source("com.example.faces", "ProjectListBean", "");
        TemplateUtil.getInstance().createManagedBeanCrudFile(mock(Log.class), Map.of(
            "packageName", "com.example.faces",
            "className", "ProjectListBean",
            "modelName", "Project",
            "instanceModelName", "project",
            "idName", "id",
            "importsList", Set.of(
                "com.example.model.Project",
                "com.example.repository.ProjectRepository"),
            "relations", List.of(),
            "enumFields", List.of()), generatedBean);

        var generatedSource = Files.readString(generatedBean);
        assertTrue(generatedSource.contains("this.selectedProjects = new ArrayList<>();"));
        assertTrue(generatedSource.contains("if (this.selectedProjects != null)"));
        assertFalse(generatedSource.contains("this.selectedProjects = null;"));

        List<Path> sources = new ArrayList<>();
        sources.add(generatedBean);
        sources.add(source("com.example.model", "Project", """
            package com.example.model;
            public class Project {
                private Long id;
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
            }
            """));
        sources.add(source("com.example.repository", "ProjectRepository", """
            package com.example.repository;
            import com.example.model.Project;
            import java.util.List;
            public interface ProjectRepository {
                List<Project> findAll();
                Project save(Project project);
                void delete(Project project);
                void deleteAll(List<Project> projects);
            }
            """));
        sources.add(source("jakarta.annotation", "PostConstruct",
            "package jakarta.annotation; public @interface PostConstruct {}"));
        sources.add(source("jakarta.inject", "Named",
            "package jakarta.inject; public @interface Named {}"));
        sources.add(source("jakarta.inject", "Inject",
            "package jakarta.inject; public @interface Inject {}"));
        sources.add(source("jakarta.faces.view", "ViewScoped",
            "package jakarta.faces.view; public @interface ViewScoped {}"));
        sources.add(source("jakarta.faces.application", "FacesMessage", """
            package jakarta.faces.application;
            public class FacesMessage { public FacesMessage(String message) {} }
            """));
        sources.add(source("jakarta.faces.context", "FacesContext", """
            package jakarta.faces.context;
            import jakarta.faces.application.FacesMessage;
            public class FacesContext {
                private static final FacesContext INSTANCE = new FacesContext();
                public static FacesContext getCurrentInstance() { return INSTANCE; }
                public void addMessage(String clientId, FacesMessage message) {}
            }
            """));
        sources.add(source("jakarta.faces.component", "UIComponent",
            "package jakarta.faces.component; public class UIComponent {}"));
        sources.add(source("jakarta.faces.convert", "Converter", """
            package jakarta.faces.convert;
            import jakarta.faces.component.UIComponent;
            import jakarta.faces.context.FacesContext;
            public interface Converter<T> {
                T getAsObject(FacesContext context, UIComponent component, String value);
                String getAsString(FacesContext context, UIComponent component, T value);
            }
            """));
        sources.add(source("jakarta.faces.convert", "ConverterException", """
            package jakarta.faces.convert;
            public class ConverterException extends RuntimeException {
                public ConverterException(String message) { super(message); }
            }
            """));
        sources.add(source("org.primefaces", "PrimeFaces", """
            package org.primefaces;
            public class PrimeFaces {
                private static final PrimeFaces INSTANCE = new PrimeFaces();
                public static PrimeFaces current() { return INSTANCE; }
                public Ajax ajax() { return new Ajax(); }
                public void executeScript(String script) {}
                public static class Ajax { public void update(String... ids) {} }
            }
            """));

        Path compiledClasses = compile(sources);
        assertDeleteSequence(compiledClasses);
    }

    private void assertDeleteSequence(Path compiledClasses) throws Exception {
        try (var loader = new URLClassLoader(new java.net.URL[]{compiledClasses.toUri().toURL()})) {
            var beanClass = loader.loadClass("com.example.faces.ProjectListBean");
            var projectClass = loader.loadClass("com.example.model.Project");
            var repositoryClass = loader.loadClass("com.example.repository.ProjectRepository");
            var bulkDeletes = new AtomicInteger();
            var individualDeletes = new AtomicInteger();
            var repository = Proxy.newProxyInstance(loader, new Class<?>[]{repositoryClass},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "findAll" -> List.of();
                    case "save" -> arguments[0];
                    case "deleteAll" -> {
                        bulkDeletes.incrementAndGet();
                        yield null;
                    }
                    case "delete" -> {
                        individualDeletes.incrementAndGet();
                        yield null;
                    }
                    default -> null;
                });

            var bean = beanClass.getConstructor().newInstance();
            var repositoryField = beanClass.getDeclaredField("projectRepository");
            repositoryField.setAccessible(true);
            repositoryField.set(bean, repository);
            beanClass.getMethod("init").invoke(bean);

            var first = projectClass.getConstructor().newInstance();
            var second = projectClass.getConstructor().newInstance();
            beanClass.getMethod("setSelectedProjects", List.class)
                .invoke(bean, new ArrayList<>(List.of(first, second)));
            beanClass.getMethod("deleteSelectedProjects").invoke(bean);

            var selection = (List<?>) beanClass.getMethod("getSelectedProjects").invoke(bean);
            assertNotNull(selection);
            assertTrue(selection.isEmpty());

            var third = projectClass.getConstructor().newInstance();
            beanClass.getMethod("setCurrentProject", projectClass).invoke(bean, third);
            beanClass.getMethod("deleteProject").invoke(bean);

            beanClass.getMethod("setSelectedProjects", List.class).invoke(bean, new Object[]{null});
            beanClass.getMethod("setCurrentProject", projectClass).invoke(bean, third);
            beanClass.getMethod("deleteProject").invoke(bean);

            assertEquals(1, bulkDeletes.get());
            assertEquals(2, individualDeletes.get());
        }
    }

    private Path compile(List<Path> sources) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run with a JDK");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        Path output = tempDirectory.resolve("compiled");
        Files.createDirectories(output);
        try (var fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var units = fileManager.getJavaFileObjectsFromFiles(
                sources.stream().map(Path::toFile).toList());
            boolean compiled = compiler.getTask(null, fileManager, diagnostics,
                List.of("-d", output.toString()), null, units).call();
            assertTrue(compiled, () -> "Generated CRUD bean did not compile: "
                + diagnostics.getDiagnostics());
        }
        return output;
    }

    private Path source(String packageName, String className, String contents) throws Exception {
        Path path = tempDirectory.resolve("sources")
            .resolve(packageName.replace('.', '/'))
            .resolve(className + ".java");
        Files.createDirectories(path.getParent());
        if (!contents.isEmpty()) {
            Files.writeString(path, contents);
        }
        return path;
    }
}
