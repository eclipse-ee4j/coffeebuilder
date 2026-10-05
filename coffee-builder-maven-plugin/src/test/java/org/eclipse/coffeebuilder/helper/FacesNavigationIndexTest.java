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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.dom4j.DocumentHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.coffeebuilder.util.Constants.FACES_NS_HTML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FacesNavigationIndexTest {

    @TempDir
    Path tempDirectory;

    @Test
    void createsValidStandaloneFacesIndexWhenMissing() throws Exception {
        var project = createProject(tempDirectory.resolve("create"));

        FacesNavigationIndex.getInstance().ensureExists(project, mock(Log.class));

        var index = indexPath(project);
        var contents = Files.readString(index);
        var document = DocumentHelper.parseText(contents);
        assertTrue(contents.contains(FacesNavigationIndex.MANAGED_MARKER));
        assertEquals("html", document.getRootElement().getName());
        assertEquals("http://www.w3.org/1999/xhtml", document.getRootElement().getNamespaceURI());
        assertEquals(FACES_NS_HTML,
            document.getRootElement().getNamespaceForPrefix("h").getURI());
        assertTrue(contents.contains("<title>Application pages</title>"));
        assertFalse(contents.contains("<h:title>"));
        assertTrue(contents.contains("id=\"coffee-builder-navigation\""));
    }

    @Test
    void addsMultipleLinksAndKeepsRepeatedRegistrationIdempotent() throws Exception {
        var project = createProject(tempDirectory.resolve("links"));
        var log = mock(Log.class);
        var navigation = FacesNavigationIndex.getInstance();

        navigation.registerPage(project, log, "PageA", null);
        navigation.registerPage(project, log, "admin/PageB.xhtml", "Second page");
        navigation.registerPage(project, log, "PageA", "Ignored duplicate");

        var contents = Files.readString(indexPath(project));
        assertEquals(1, StringUtils.countMatches(contents, "outcome=\"/PageA.xhtml\""));
        assertTrue(contents.contains("value=\"Page A\""));
        assertTrue(contents.contains("outcome=\"/admin/PageB.xhtml\""));
        assertTrue(contents.contains("value=\"Second page\""));
    }

    @Test
    void preservesUnmanagedIndexAndWarnsWhenRegistrationIsSkipped() throws Exception {
        var project = createProject(tempDirectory.resolve("owned-by-user"));
        var index = indexPath(project);
        Files.createDirectories(index.getParent());
        var userContents = "<html><body>User home</body></html>";
        Files.writeString(index, userContents);
        var log = mock(Log.class);

        FacesNavigationIndex.getInstance().ensureExists(project, log);
        FacesNavigationIndex.getInstance().registerPage(project, log, "PageA", "Page A");

        assertEquals(userContents, Files.readString(index));
        verify(log).warn(contains("not managed by Coffee Builder"));
    }

    @Test
    void facePageGenerationMaintainsTheIndexIncrementally() throws Exception {
        var project = createProject(tempDirectory.resolve("face-pages"));
        var log = mock(Log.class);
        var faces = JakartaFacesHelper.getInstance();

        faces.addFacePage(project, log, "PageA", false);
        faces.addFacePage(project, log, "PageB", false);
        faces.addFacePage(project, log, "PageA", false);

        assertTrue(Files.exists(project.getBasedir().toPath()
            .resolve("src/main/webapp/PageA.xhtml")));
        var contents = Files.readString(indexPath(project));
        assertEquals(1, StringUtils.countMatches(contents, "outcome=\"/PageA.xhtml\""));
        assertEquals(1, StringUtils.countMatches(contents, "outcome=\"/PageB.xhtml\""));
    }

    private MavenProject createProject(Path directory) throws Exception {
        Files.createDirectories(directory);
        var model = new Model();
        model.setGroupId("com.example");
        model.setArtifactId("faces-test");
        var project = new MavenProject(model);
        project.setFile(directory.resolve("pom.xml").toFile());
        return project;
    }

    private Path indexPath(MavenProject project) {
        return project.getBasedir().toPath().resolve("src/main/webapp/index.xhtml");
    }
}
