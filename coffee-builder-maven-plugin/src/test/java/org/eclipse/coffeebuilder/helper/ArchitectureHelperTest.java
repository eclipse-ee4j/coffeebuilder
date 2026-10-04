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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.coffeebuilder.util.TemplateUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ArchitectureHelperTest {

    @TempDir
    Path tempDirectory;

    @Test
    void createsCompilableDomainModelWithEntityEnumsAndLists() throws Exception {
        var project = createProject();
        var log = mock(Log.class);
        var definitions = readJson("""
            {
              "Project": {
                "fields": {
                  "id": {"type": "UUID", "isId": true},
                  "name": {"type": "String"}
                }
              },
              "Issue": {
                "fields": {
                  "id": {"type": "UUID", "isId": true},
                  "project": {"type": "Project", "ManyToOne": {}},
                  "type": {"type": "enum", "values": ["TASK", "BUG", "FEATURE"]},
                  "status": {"type": "enum", "values": ["TODO", "IN_PROGRESS", "DONE"]},
                  "priority": {"type": "enum", "values": ["LOW", "MEDIUM", "HIGH"]},
                  "labels": {"type": "String", "list": true},
                  "createdAt": {"type": "LocalDateTime"}
                }
              }
            }
            """);

        ArchitectureHelper.getInstance().createDtos(project, log, definitions);

        var issueSource = Files.readString(javaSource("com.example.tracker.domain.model", "Issue"));
        assertTrue(issueSource.contains("import com.example.tracker.enums.IssueEntityType;"));
        assertTrue(issueSource.contains("import com.example.tracker.enums.IssueEntityStatus;"));
        assertTrue(issueSource.contains("import com.example.tracker.enums.IssueEntityPriority;"));
        assertEquals(1, issueSource.lines().filter("import java.util.List;"::equals).count());
        assertTrue(issueSource.contains("private IssueEntityType type;"));
        assertTrue(issueSource.contains("private IssueEntityStatus status;"));
        assertTrue(issueSource.contains("private IssueEntityPriority priority;"));
        assertTrue(issueSource.contains("private List<String> labels;"));
        assertTrue(issueSource.contains("private Project project;"));
        assertTrue(issueSource.contains("private LocalDateTime createdAt;"));
        assertTrue(issueSource.contains("public IssueEntityType getType()"));
        assertTrue(issueSource.contains("public void setType(IssueEntityType type)"));
        assertTrue(issueSource.contains("public List<String> getLabels()"));
        assertTrue(issueSource.contains("public void setLabels(List<String> labels)"));

        createEnum(log, "IssueEntityType", List.of("TASK", "BUG", "FEATURE"));
        createEnum(log, "IssueEntityStatus", List.of("TODO", "IN_PROGRESS", "DONE"));
        createEnum(log, "IssueEntityPriority", List.of("LOW", "MEDIUM", "HIGH"));
        assertGeneratedSourcesCompile();
    }

    private MavenProject createProject() {
        var model = new Model();
        model.setGroupId("com.example");
        model.setArtifactId("tracker");
        var project = new MavenProject(model);
        project.setFile(tempDirectory.resolve("pom.xml").toFile());
        return project;
    }

    private JsonObject readJson(String content) {
        try (var reader = Json.createReader(new StringReader(content))) {
            return reader.readObject();
        }
    }

    private void createEnum(Log log, String name, List<String> values) throws Exception {
        TemplateUtil.getInstance().createEnumFile(log,
            Map.of("packageName", "com.example.tracker.enums", "className", name, "values", values),
            javaSource("com.example.tracker.enums", name));
    }

    private Path javaSource(String packageName, String className) {
        return tempDirectory.resolve("src/main/java")
            .resolve(packageName.replace('.', '/'))
            .resolve(className + ".java");
    }

    private void assertGeneratedSourcesCompile() throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run with a JDK");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var sources = Files.walk(tempDirectory.resolve("src/main/java"))
            .filter(path -> path.toString().endsWith(".java"))
            .map(Path::toFile)
            .toList();
        var output = tempDirectory.resolve("compiled");
        Files.createDirectories(output);

        try (var fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var compilationUnits = fileManager.getJavaFileObjectsFromFiles(sources);
            var compiled = compiler.getTask(null, fileManager, diagnostics,
                List.of("-d", output.toString()), null, compilationUnits).call();
            assertTrue(compiled, () -> "Generated sources did not compile: " + diagnostics.getDiagnostics());
        }
    }
}
