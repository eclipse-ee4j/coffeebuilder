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

import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PrimeFacesHelperTest {

    @TempDir
    Path tempDirectory;

    @Test
    void generatesTypedIssueCrudFromEntitySemanticsAndFormPresentation() throws Exception {
        var entities = write("entities.json", """
            {
              "Project": {"fields": {
                "id": {"type": "Long", "isId": true},
                "name": {"type": "String"}
              }},
              "Milestone": {"fields": {
                "id": {"type": "Long", "isId": true},
                "title": {"type": "String"}
              }},
              "Issue": {"fields": {
                "id": {"type": "Long", "isId": true},
                "title": {"type": "String"},
                "description": {"type": "String"},
                "estimate": {"type": "Integer"},
                "dueDate": {"type": "LocalDate"},
                "createdAt": {"type": "LocalDateTime"},
                "status": {"type": "enum", "values": ["OPEN", "CLOSED"]},
                "project": {"type": "Project", "manyToOne": true},
                "milestone": {"type": "Milestone", "manyToOne": true},
                "labels": {"type": "String", "list": true}
              }}
            }
            """);
        var forms = write("forms.json", """
            {
              "IssueList": {
                "entity": "Issue",
                "title": "Issues",
                "template": {"facelet": "/WEB-INF/templates/main.xhtml", "define": "body"},
                "fields": {
                  "id": {"label": "Id"},
                  "title": {"label": "Title", "component": "inputText"},
                  "description": {"label": "Description", "component": "textarea"},
                  "estimate": {"label": "Estimate", "component": "inputNumber"},
                  "dueDate": {"label": "Due date", "component": "datePicker"},
                  "createdAt": {"label": "Created"},
                  "status": {"label": "Status", "component": "selectOneMenu"},
                  "project": {"label": "Project", "displayField": "name"},
                  "milestone": {"label": "Milestone"},
                  "labels": {"label": "Labels"}
                }
              }
            }
            """);

        PrimeFacesHelper.getInstance().addFormsFromEntities(createProject(), mock(Log.class), forms, entities);

        var xhtml = Files.readString(tempDirectory.resolve("src/main/webapp/IssueList.xhtml"));
        assertTrue(xhtml.contains("<p:inputText id=\"title\""));
        assertTrue(xhtml.contains("<p:inputTextarea id=\"description\""));
        assertTrue(xhtml.contains("<p:inputNumber id=\"estimate\""));
        assertTrue(xhtml.contains("<p:datePicker  id=\"dueDate\"")
            || xhtml.contains("<p:datePicker id=\"dueDate\""));
        assertTrue(xhtml.contains("id=\"createdAt\"") && xhtml.contains("showTime=\"true\""));
        assertTrue(xhtml.contains("<p:selectOneMenu id=\"status\""));
        assertTrue(xhtml.contains("value=\"#{issueListBean.statusValues}\""));
        assertTrue(xhtml.contains("<p:selectOneMenu id=\"project\""));
        assertTrue(xhtml.contains("itemLabel=\"#{projectOption.name}\""));
        assertTrue(xhtml.contains("itemLabel=\"#{milestoneOption.title}\""));
        assertTrue(xhtml.contains("<p:chips id=\"labels\""));

        var bean = Files.readString(javaSource("IssueListBean"));
        assertTrue(bean.contains("private ProjectRepository projectRepository;"));
        assertTrue(bean.contains("private MilestoneRepository milestoneRepository;"));
        assertTrue(bean.contains("this.projects = projectRepository.findAll();"));
        assertTrue(bean.contains("this.milestones = milestoneRepository.findAll();"));
        assertTrue(bean.contains("public Converter<Project> getProjectConverter()"));
        assertTrue(bean.contains("return IssueEntityStatus.values();"));
        assertTrue(bean.contains("currentIssue = issueRepository.save(currentIssue);"));
        assertTrue(bean.indexOf("issueRepository.save(currentIssue)")
            < bean.indexOf("if (newModel)"));
    }

    @Test
    void existingSimpleProjectFormStillGenerates() throws Exception {
        var entities = write("simple-entities.json", """
            {"Project": {"fields": {
              "id": {"type": "Long", "isId": true},
              "name": {"type": "String"},
              "internalCode": {"type": "String"},
              "startDate": {"type": "LocalDate"}
            }}}
            """);
        var forms = write("simple-forms.json", """
            {"ProjectList": {
              "entity": "Project",
              "template": {"facelet": "/WEB-INF/templates/main.xhtml", "define": "body"},
              "fields": {
                "id": {"label": "Id"},
                "name": {"label": "Name"},
                "startDate": {"label": "Start date"}
              }
            }}
            """);

        PrimeFacesHelper.getInstance().addFormsFromEntities(createProject(), mock(Log.class), forms, entities);

        var xhtml = Files.readString(tempDirectory.resolve("src/main/webapp/ProjectList.xhtml"));
        assertTrue(xhtml.contains("<p:inputNumber id=\"id\""));
        assertTrue(xhtml.contains("<p:inputText id=\"name\""));
        assertTrue(xhtml.contains("id=\"startDate\""));
        assertFalse(xhtml.contains("id=\"internalCode\""));
        assertTrue(Files.exists(javaSource("ProjectListBean")));
    }

    private MavenProject createProject() {
        var model = new Model();
        model.setGroupId("com.example");
        model.setArtifactId("tracker");
        var project = new MavenProject(model);
        project.setFile(tempDirectory.resolve("pom.xml").toFile());
        return project;
    }

    private Path write(String name, String content) throws Exception {
        var path = tempDirectory.resolve(name);
        Files.writeString(path, content);
        return path;
    }

    private Path javaSource(String className) {
        return tempDirectory.resolve("src/main/java/com/example/tracker/app/faces")
            .resolve(className + ".java");
    }
}
