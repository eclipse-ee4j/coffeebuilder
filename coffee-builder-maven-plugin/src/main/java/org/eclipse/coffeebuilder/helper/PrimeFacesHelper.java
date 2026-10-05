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
package org.eclipse.coffeebuilder.helper;

import org.eclipse.coffeebuilder.util.CoffeeBuilderUtil;
import org.eclipse.coffeebuilder.util.JsonUtil;
import org.eclipse.coffeebuilder.util.MavenProjectUtil;
import org.eclipse.coffeebuilder.util.PathsUtil;
import org.eclipse.coffeebuilder.util.StringsUtil;
import org.eclipse.coffeebuilder.util.TemplateUtil;
import org.eclipse.coffeebuilder.util.XmlUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.eclipse.coffeebuilder.util.Constants.CLASS_NAME;
import static org.eclipse.coffeebuilder.util.Constants.ENTITY;
import static org.eclipse.coffeebuilder.util.Constants.FIELDS;
import static org.eclipse.coffeebuilder.util.Constants.MODEL_NAME;
import static org.eclipse.coffeebuilder.util.Constants.PACKAGE_NAME;
import static org.eclipse.coffeebuilder.util.Constants.TYPE;

/**
 * Helper class for generating PrimeFaces-related code and configurations.
 * This class extends {@link JakartaFacesHelper} and provides methods for
 * creating forms, managed beans, and managing message bundles specific to PrimeFaces.
 * It interacts with various utility classes to read JSON definitions,
 * generate files from templates, and manipulate XML documents.
 */
public class PrimeFacesHelper extends JakartaFacesHelper {

    private static final Set<String> SUPPORTED_COMPONENTS = Set.of(
        "inputText", "textarea", "inputNumber", "datePicker", "selectOneMenu");

    private static final Set<String> NUMERIC_TYPES = Set.of(
        "byte", "short", "int", "long", "float", "double",
        "Byte", "Short", "Integer", "Long", "Float", "Double", "BigInteger", "BigDecimal");

    /**
     * PrimeFaces XML namespace for the "p" prefix.
     */
    protected static final Namespace PRIMEFACES_NS_P_NAMESPACE = new Namespace("p", "primefaces");

    /**
     * Name of the messages properties file.
     */
    private static final String MESSAGES_PROPERTIES = "messages.properties";


    Predicate<Map.Entry<String, JsonValue>> entryFilter = entry -> entry.getValue()
        .getValueType() == JsonValue.ValueType.OBJECT
        && entry.getValue().asJsonObject().containsKey(ENTITY);

    private PrimeFacesHelper() {

    }

    /**
     * Returns the singleton instance of {@code PrimeFacesHelper}.
     *
     * @return The singleton instance of {@code PrimeFacesHelper}.
     */
    public static PrimeFacesHelper getInstance() {
        return PrimeFacesUtilHolder.INSTANCE;
    }

    /**
     * Adds forms from entity definitions by processing JSON files and generating necessary resources.
     *
     * @param mavenProject the Maven project context
     * @param log          the logger for logging messages and debug information
     * @param formsPath    the file path to the forms definition JSON file
     * @param entitiesPth  the file path to the entities definition JSON file
     * @throws IOException if an I/O error occurs during file reading or processing
     */
    public void addFormsFromEntities(MavenProject mavenProject,
                                     Log log,
                                     Path formsPath,
                                     Path entitiesPth) throws IOException {
        var formsJson = JsonUtil.readJsonValue(formsPath).asJsonObject();
        var entitiesJson = JsonUtil.readJsonValue(entitiesPth).asJsonObject();
        var webAppPath = PathsUtil.getWebappPath(mavenProject);
        var jakartaEeHelper = JakartaEeHelper.getInstance();

        var properties = getMessagesBundle(mavenProject);

        for (var entry : formsJson.entrySet().stream().filter(entryFilter).toList()) {
            createFormFromEntity(mavenProject, log, entry, properties, entitiesJson,
                jakartaEeHelper, webAppPath);
        }

        saveMessagesBundle(mavenProject, log, properties);

    }

    private void createFormFromEntity(MavenProject mavenProject,
                                      Log log,
                                      Map.Entry<String, JsonValue> entry,
                                      Properties properties,
                                      JsonObject entitiesJson,
                                      JakartaEeHelper jakartaEeHelper,
                                      Path webAppPath) throws IOException {
        var formName = entry.getKey();
        var formDescription = entry.getValue().asJsonObject();
        createMessagesBundle(log, formDescription, properties);
        var base = formDescription.getString("base", "/");
        var pageName = StringsUtil.removeCharacterRoot(base + formName);
        var entityName = formDescription.getString(ENTITY);
        var entityDescription = entitiesJson.getJsonObject(entityName);
        if (entityDescription == null) {
            throw new IOException("Entity definition not found: " + entityName);
        }
        var fieldIdDefinition = getFieldIdDefinition(entityDescription);
        var fields = createFieldDefinitions(entityName, formDescription, entityDescription, entitiesJson);
        var relations = createRelationDefinitions(fields);
        var enumFields = fields.stream().filter(field -> Boolean.TRUE.equals(field.get("enum"))).toList();
        jakartaEeHelper.createDomain(mavenProject, entityName, entityDescription);
        createManagedBean(mavenProject, log, pageName, entityName, fieldIdDefinition, relations, enumFields);
        createForm(log, webAppPath, formName, pageName, formDescription, entityDescription,
            fieldIdDefinition, fields, relations);
        FacesNavigationIndex.getInstance().registerPage(mavenProject, log, pageName,
            formDescription.getString("title",
                FacesNavigationIndex.getInstance().readableLabel(pageName)));
    }

    private void saveMessagesBundle(MavenProject mavenProject, Log log, Properties properties) throws IOException {
        log.debug("Saving messages bundle");
        var messagesBundlePath = PathsUtil.getResourcePath(mavenProject).resolve(MESSAGES_PROPERTIES);
        try (FileWriter writer = new FileWriter(messagesBundlePath.toFile())) {
            properties.store(writer, null);
        }
    }

    private Properties getMessagesBundle(MavenProject mavenProject) throws IOException {
        var messagesBundlePath = PathsUtil.getResourcePath(mavenProject).resolve(MESSAGES_PROPERTIES);
        Properties properties = new Properties();
        if (Files.exists(messagesBundlePath))
            try (FileReader reader = new FileReader(messagesBundlePath.toFile())) {
                properties.load(reader);
            }
        properties.putIfAbsent("app_save", "Save");
        properties.putIfAbsent("app_cancel", "Cancel");
        properties.putIfAbsent("app_new", "New");
        properties.putIfAbsent("yes", "Yes");
        properties.putIfAbsent("no", "No");
        properties.putIfAbsent("confirm", "Confirm");
        return properties;
    }

    private Map<String, String> getFieldIdDefinition(JsonObject entityDefinition) {
        return CoffeeBuilderUtil.getFieldId(entityDefinition)
            .map(fieldId -> Map.ofEntries(
                Map.entry("idName", fieldId.getKey()),
                Map.entry("idType", fieldId.getValue().asJsonObject().getString(TYPE))
            )).orElse(Map.of());
    }

    /**
     * Creates a PrimeFaces managed bean for a given entity.
     *
     * @param mavenProject      The Maven project context.
     * @param log               The logger for logging messages.
     * @param pageName          The name of the JSF page associated with the managed bean.
     * @param entityName        The name of the entity for which the managed bean is created.
     * @param fieldIdDefinition A map containing the ID field's name and type for the entity.
     * @throws IOException If an I/O error occurs during file creation.
     */
    public void createManagedBean(MavenProject mavenProject,
                                  Log log,
                                  String pageName,
                                  String entityName,
                                  Map<String, String> fieldIdDefinition,
                                  List<Map<String, Object>> relations,
                                  List<Map<String, Object>> enumFields) throws IOException {
        var packageDefinition = MavenProjectUtil.getFacesPackage(mavenProject);
        var className = StringsUtil.toPascalCase(pageName) + "Bean";
        var managedBeanPath = PathsUtil.getJavaPath(mavenProject, packageDefinition, className);
        Set<String> importsList = new LinkedHashSet<>(List.of(
            "%s.%sRepository".formatted(MavenProjectUtil.getModelRepositoryPackage(mavenProject), entityName),
            "%s.%s".formatted(MavenProjectUtil.getModelPackage(mavenProject), entityName)
        ));
        relations.forEach(relation -> {
            var type = relation.get("type");
            importsList.add("%s.%sRepository".formatted(MavenProjectUtil.getModelRepositoryPackage(mavenProject), type));
            importsList.add("%s.%s".formatted(MavenProjectUtil.getModelPackage(mavenProject), type));
        });
        enumFields.forEach(field -> importsList.add(
            "%s.%s".formatted(MavenProjectUtil.getEnumsPackage(mavenProject), field.get("enumType"))));
        Map<String, Object> fieldsMap = new LinkedHashMap<>(Map.ofEntries(
            Map.entry(PACKAGE_NAME, packageDefinition),
            Map.entry(MODEL_NAME, entityName),
            Map.entry(CLASS_NAME, className),
            Map.entry("instanceModelName", StringUtils.uncapitalize(entityName)),
            Map.entry("importsList", importsList),
            Map.entry("relations", relations),
            Map.entry("enumFields", enumFields)
        ));
        fieldsMap.putAll(fieldIdDefinition);

        TemplateUtil.getInstance().createManagedBeanCrudFile(log, fieldsMap, managedBeanPath);
    }

    private void createMessagesBundle(Log log, JsonObject formsJson, Properties properties) {
        var formEntityName = formsJson.getString(ENTITY);
        var formEntityNameLowerCase = StringUtils.lowerCase(formEntityName);
        log.debug("Creating messages bundle for " + formEntityName);
        properties.putIfAbsent("delete_confirm_%s".formatted(formEntityNameLowerCase),
            "Confirm delete %s ?".formatted(formEntityName));
        properties.putIfAbsent("delete_confirm_%ss".formatted(formEntityNameLowerCase),
            "Confirm delete %ss ?".formatted(formEntityName));
        var bundleMessages = formsJson.getJsonObject(FIELDS)
            .entrySet().stream().map(entry -> Map.entry(formEntityName + "_" + entry.getKey(), entry.getValue()
                .asJsonObject().getString("label", entry.getKey())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        bundleMessages.forEach(properties::setProperty);

    }

    private void createForm(Log log,
                            Path webAppPath,
                            String formName,
                            String pageName,
                            JsonObject formDescription,
                            JsonObject entityDescription,
                            Map<String, String> fieldIdDefinition,
                            List<Map<String, Object>> fields,
                            List<Map<String, Object>> relations) throws IOException {
        var pagePath = webAppPath.resolve(pageName + ".xhtml");
        var title = formDescription.getString("title", formName);
        var templateDesc = formDescription.getJsonObject("template");
        var entityName = formDescription.getString(ENTITY);
        var formIdName = StringUtils.uncapitalize(entityName) + "Form";

        var pageXhtml = templateDesc == null
            ? createFacePage(log, pagePath, entityDescription, formIdName)
            : createFacePageWithTemplate(log,
            pagePath,
            templateDesc,
            entityDescription,
            entityName,
            title,
            fieldIdDefinition,
            fields,
            relations);
        XmlUtil.getInstance().saveDocument(pageXhtml, log, pagePath);
    }

    private Document createFacePage(Log log, Path xhtml, JsonObject entityDefinition, String formIdName) {
        return createFacePage(log, xhtml, (bodyElement) -> createForm(log, bodyElement, entityDefinition, formIdName));
    }

    private Document createFacePageWithTemplate(Log log,
                                                Path xhtml,
                                                JsonObject templateDesc,
                                                JsonObject entityDefinition,
                                                String entityName,
                                                String title,
                                                Map<String, String> fieldIdDefinition,
                                                List<Map<String, Object>> fields,
                                                List<Map<String, Object>> relations)
        throws IOException {
        String templateFacelet = templateDesc.getString("facelet");
        String define = templateDesc.getString("define");

        Map<String, Object> fieldsMap = new LinkedHashMap<>(Map.of(
            "define", define,
            "template_name", templateFacelet,
            "instanceModelName", StringUtils.uncapitalize(entityName),
            MODEL_NAME, entityName,
            "fields", fields,
            "title", title,
            "relations", relations
        ));
        fieldsMap.putAll(fieldIdDefinition);

        TemplateUtil.getInstance().createFacesCrudFile(log, fieldsMap, xhtml);

        return null;
    }

    private void createForm(Log log, Element defineTag, JsonObject entityDefinition, String formIdName) {
        log.debug("creating content form in element:" + defineTag);
        var xmlUtil = XmlUtil.getInstance();
        var formElement = xmlUtil.addElement(defineTag, "form", FACES_NS_HTML_NAMESPACE)
            .addAttribute("id", formIdName);
        var cardElement = xmlUtil.addElement(formElement, "card", PRIMEFACES_NS_P_NAMESPACE);
        entityDefinition.getJsonObject(FIELDS).forEach((fieldName, fieldDef) -> {
            var fieldDefinition = fieldDef.asJsonObject();

            var panelGroupElement = xmlUtil.addElement(cardElement, "panelGroup", FACES_NS_HTML_NAMESPACE)
                .addAttribute("layout", "block")
                .addAttribute("styleClass", "field");

            var textLabel = fieldDefinition.getString("label", fieldName);
            var outputLabel = xmlUtil.addElement(panelGroupElement, "outputLabel", PRIMEFACES_NS_P_NAMESPACE)
                .addAttribute("for", fieldName)
                .addAttribute("value", textLabel);

            var elementInput = getElementInputByType(fieldDefinition.getString(TYPE));

            var inputText = xmlUtil.addElement(panelGroupElement, elementInput, PRIMEFACES_NS_P_NAMESPACE)
                .addAttribute("id", fieldName);
        });
    }

    private String getElementInputByType(String fieldType) {
        return switch (fieldType) {
            case "String" -> "inputText";
            case "Integer", "Long" -> "inputNumber";
            case "LocalDate" -> "datePicker";
            default -> "inputText";
        };
    }

    private List<Map<String, Object>> createFieldDefinitions(String entityName,
                                                              JsonObject formDescription,
                                                              JsonObject entityDescription,
                                                              JsonObject entitiesJson) throws IOException {
        var formFields = formDescription.getJsonObject(FIELDS);
        if (formFields == null) {
            throw new IOException("Form fields not found for entity " + entityName);
        }
        var entityFields = entityDescription.getJsonObject(FIELDS);
        var result = new java.util.ArrayList<Map<String, Object>>();
        for (var entry : formFields.entrySet()) {
            var fieldName = entry.getKey();
            var entityField = entityFields.getJsonObject(fieldName);
            if (entityField == null) {
                throw new IOException("Field %s is not defined by entity %s".formatted(fieldName, entityName));
            }
            var presentation = entry.getValue().asJsonObject();
            var type = entityField.getString(TYPE);
            var list = entityField.getBoolean("list", false);
            var enumField = "enum".equals(type);
            var manyToOne = isManyToOne(entityField);
            var inferredComponent = inferComponent(type, list, enumField, manyToOne);
            var component = presentation.getString("component", inferredComponent);
            validateComponent(entityName, fieldName, type, component, enumField, manyToOne, list);

            Map<String, Object> field = new LinkedHashMap<>();
            field.put("name", fieldName);
            field.put("type", type);
            field.put("component", component);
            field.put("enum", enumField);
            field.put("manyToOne", manyToOne);
            field.put("list", list);
            if (enumField) {
                field.put("enumType", JakartaPersistenceHelper.enumClassName(entityName + "Entity", fieldName));
                field.put("valuesProperty", fieldName + "Values");
            }
            if (manyToOne) {
                addRelationDefinition(field, presentation, type, entitiesJson);
            }
            result.add(field);
        }
        return result;
    }

    private String inferComponent(String type, boolean list, boolean enumField, boolean manyToOne)
        throws IOException {
        if (list) {
            if ("String".equals(type)) {
                return "chips";
            }
            throw new IOException("Only List<String> fields are supported by CRUD form generation");
        }
        if (enumField || manyToOne) {
            return "selectOneMenu";
        }
        if (NUMERIC_TYPES.contains(type)) {
            return "inputNumber";
        }
        return switch (type) {
            case "LocalDate", "LocalDateTime" -> "datePicker";
            default -> "inputText";
        };
    }

    private void validateComponent(String entityName,
                                   String fieldName,
                                   String type,
                                   String component,
                                   boolean enumField,
                                   boolean manyToOne,
                                   boolean list) throws IOException {
        if (!SUPPORTED_COMPONENTS.contains(component) && !"chips".equals(component)) {
            throw new IOException("Unsupported component '%s' for %s.%s"
                .formatted(component, entityName, fieldName));
        }
        if ((enumField || manyToOne) && !"selectOneMenu".equals(component)) {
            throw new IOException("Enum and many-to-one fields require selectOneMenu: "
                + entityName + "." + fieldName);
        }
        if ("selectOneMenu".equals(component) && !enumField && !manyToOne) {
            throw new IOException("selectOneMenu requires an enum or many-to-one field: "
                + entityName + "." + fieldName);
        }
        if (list && !"chips".equals(component)) {
            throw new IOException("List field requires chips component: " + entityName + "." + fieldName);
        }
        if ("textarea".equals(component) && !"String".equals(type)) {
            throw new IOException("textarea requires a String field: " + entityName + "." + fieldName);
        }
        if ("inputNumber".equals(component) && !NUMERIC_TYPES.contains(type)) {
            throw new IOException("inputNumber requires a numeric field: " + entityName + "." + fieldName);
        }
        if ("datePicker".equals(component)
            && !Set.of("LocalDate", "LocalDateTime").contains(type)) {
            throw new IOException("datePicker requires a LocalDate or LocalDateTime field: "
                + entityName + "." + fieldName);
        }
    }

    private boolean isManyToOne(JsonObject entityField) {
        return entityField.entrySet().stream()
            .filter(entry -> "manyToOne".equalsIgnoreCase(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .map(value -> value == JsonValue.TRUE || value.getValueType() == JsonValue.ValueType.OBJECT)
            .orElse(false);
    }

    private void addRelationDefinition(Map<String, Object> field,
                                       JsonObject presentation,
                                       String relatedType,
                                       JsonObject entitiesJson) throws IOException {
        var relatedEntity = entitiesJson.getJsonObject(relatedType);
        if (relatedEntity == null) {
            throw new IOException("Related entity definition not found: " + relatedType);
        }
        var relatedFields = relatedEntity.getJsonObject(FIELDS);
        var displayField = presentation.containsKey("displayField")
            ? presentation.getString("displayField")
            : List.of("name", "title", "id").stream()
                .filter(relatedFields::containsKey)
                .findFirst()
                .orElseThrow(() -> new IOException(
                    "No display field (name, title, or id) found for related entity " + relatedType));
        if (!relatedFields.containsKey(displayField)) {
            throw new IOException("Display field %s is not defined by related entity %s"
                .formatted(displayField, relatedType));
        }
        var id = CoffeeBuilderUtil.getFieldId(relatedEntity)
            .orElseThrow(() -> new IOException("No id field found for related entity " + relatedType));
        var instanceName = StringUtils.uncapitalize(relatedType);
        field.put("relatedType", relatedType);
        field.put("relatedInstance", instanceName);
        field.put("relatedOptions", instanceName + "s");
        field.put("converterProperty", instanceName + "Converter");
        field.put("displayField", displayField);
        field.put("relatedId", id.getKey());
    }

    private List<Map<String, Object>> createRelationDefinitions(List<Map<String, Object>> fields) {
        Map<String, Map<String, Object>> relations = new LinkedHashMap<>();
        fields.stream()
            .filter(field -> Boolean.TRUE.equals(field.get("manyToOne")))
            .forEach(field -> relations.putIfAbsent((String) field.get("relatedType"), field));
        return List.copyOf(relations.values());
    }

    private static class PrimeFacesUtilHolder {

        private static final PrimeFacesHelper INSTANCE = new PrimeFacesHelper();
    }
}
