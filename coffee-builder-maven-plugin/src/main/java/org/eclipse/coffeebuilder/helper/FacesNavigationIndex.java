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

import org.eclipse.coffeebuilder.util.PathsUtil;
import org.eclipse.coffeebuilder.util.XmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.eclipse.coffeebuilder.util.Constants.FACES_NS_HTML;

/** Maintains the Coffee Builder-owned Faces navigation home page. */
public final class FacesNavigationIndex {

    static final String MANAGED_MARKER = "Eclipse Coffee Builder managed navigation index";
    private static final Namespace FACES_HTML = Namespace.get("h", FACES_NS_HTML);
    private static final String INDEX_FILE = "index.xhtml";
    private static final String NAVIGATION_ID = "coffee-builder-navigation";

    private FacesNavigationIndex() {
    }

    public static FacesNavigationIndex getInstance() {
        return Holder.INSTANCE;
    }

    /** Creates the managed index when no index exists and preserves any existing file. */
    public void ensureExists(MavenProject project, Log log) throws IOException {
        Path index = indexPath(project);
        if (Files.exists(index)) {
            return;
        }

        Document document = DocumentHelper.createDocument();
        document.addComment(MANAGED_MARKER);
        Element html = document.addElement("html", "http://www.w3.org/1999/xhtml");
        html.add(FACES_HTML);
        Element head = html.addElement(QName.get("head", FACES_HTML));
        head.addElement(QName.get("title", html.getNamespace())).setText("Application pages");
        Element body = html.addElement(QName.get("body", FACES_HTML));
        body.addElement(QName.get("outputText", FACES_HTML))
            .addAttribute("value", "Application pages");
        body.addElement(QName.get("panelGroup", FACES_HTML))
            .addAttribute("id", NAVIGATION_ID)
            .addAttribute("layout", "block");
        XmlUtil.getInstance().saveDocument(document, log, index);
        if (!Files.exists(index)) {
            throw new IOException("Unable to create Faces navigation index " + index);
        }
    }

    /** Adds a page link to a managed index, without changing a user-owned index. */
    public void registerPage(MavenProject project, Log log, String pageName, String label)
        throws IOException {
        ensureExists(project, log);
        Path index = indexPath(project);
        String contents = Files.readString(index);
        if (!contents.contains(MANAGED_MARKER)) {
            log.warn("Navigation link for %s was not added because %s is not managed by Coffee Builder"
                .formatted(pageName, index));
            return;
        }

        String outcome = outcome(pageName);
        if ("/index.xhtml".equalsIgnoreCase(outcome)) {
            return;
        }
        Document document = XmlUtil.getInstance().getDocument(log, index).orElseThrow(
            () -> new IOException("Unable to read managed Faces navigation index " + index));
        Element navigation = XmlUtil.getInstance().findElements(document,
                "//*[local-name()='panelGroup' and @id='" + NAVIGATION_ID + "']")
            .findFirst()
            .orElse(null);
        if (navigation == null) {
            throw new IOException("Managed Faces navigation container not found in " + index);
        }
        boolean alreadyRegistered = navigation.elements().stream()
            .anyMatch(element -> "link".equals(element.getName())
                && outcome.equals(element.attributeValue("outcome")));
        if (alreadyRegistered) {
            return;
        }

        navigation.addElement(QName.get("link", FACES_HTML))
            .addAttribute("outcome", outcome)
            .addAttribute("value", StringUtils.defaultIfBlank(label, readableLabel(pageName)));
        XmlUtil.getInstance().saveDocument(document, log, index);
    }

    /** Derives a readable link label from a page name. */
    public String readableLabel(String pageName) {
        String normalized = pageName.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0) {
            normalized = normalized.substring(lastSlash + 1);
        }
        normalized = Strings.CI.removeEnd(normalized, ".xhtml");
        normalized = normalized.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
            .replaceAll("[_-]+", " ")
            .trim();
        if (normalized.isEmpty()) {
            return "Page";
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private String outcome(String pageName) {
        String normalized = pageName.replace('\\', '/');
        normalized = Strings.CS.removeStart(normalized, "/");
        if (!Strings.CI.endsWith(normalized, ".xhtml")) {
            normalized += ".xhtml";
        }
        return "/" + normalized;
    }

    private Path indexPath(MavenProject project) throws IOException {
        return PathsUtil.getWebappPath(project).resolve(INDEX_FILE);
    }

    private static final class Holder {
        private static final FacesNavigationIndex INSTANCE = new FacesNavigationIndex();
    }
}
