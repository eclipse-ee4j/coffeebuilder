# Coffee Builder Maven Plugin

[English](README.md) | [Español](README.es.md)

The Coffee Builder Maven Plugin incrementally adds Jakarta EE capabilities to an existing Maven project.

**Coffee Builder generates the foundation. The developer builds the application.**

## Command-Line Usage

Plugin goals can be executed directly without adding the plugin to the project's `pom.xml`:

```bash
mvn org.eclipse.coffeebuilder:coffee-builder-maven-plugin:0.1.0-SNAPSHOT:<goal> -D<option>=<value>
```

---

## Persistence and Data

### `add-persistence`

Configures the persistence unit (JPA).

- **`-Dpersistence-unit-name=<name>`**: Persistence unit name. (Default: `defaultPU`)
- **`-Ddatasource-name=<name>`**: Data Source JNDI name. (Default: `defaultDatasource`)
- **`-Durl=<jdbc-url>`**: JDBC connection URL. (Default: H2 in-memory)
- **`-Duser=<user>`**: Database user.
- **`-Dpassword=<password>`**: Database password.
- **`-Dproperties=<name:value,...>`**: Additional Data Source properties as comma-separated `name:value` pairs.
- **`-Ddeclare=<web|...>`**: Location where the resource is declared. (Default: `web`)

### `add-datasource`

Adds Data Source configuration.

- Uses the same options as `add-persistence`.

### `add-entities`

Integrates JPA entity definitions into the project.

- **`-Dentities-file=<path>`**: Path to the entity definition file. (**Required**)

---

## Application Model

### `add-domain-models`

Generates application model layers (DTOs, mappers, repositories, and services) from an entity definition.

- **`-Dentities-file=<path>`**: Path to the JSON entity definition file. (**Required**)

### `create-openapi`

Generates server-side code from an OpenAPI specification.

- **`-Dopenapi-server=<path>`**: Path to the OpenAPI file (YAML/JSON). (Default: `${project.basedir}/openapi.yml`)

---

## Jakarta Faces

### `add-faces`

Configures Jakarta Faces in the project, including dependencies and the `web.xml` declaration.

- **`-Dwelcome-file=<name>`**: Welcome file name. (Default: `index.xhtml`)

### `add-face-page`

Adds a new Jakarta Faces page to the project.

- **`-Dname=<name>`**: Page name without the extension. (**Required**)
- **`-Dmanaged-bean=<true|false>`**: Whether to create an associated backing bean. (Default: `true`)
- **`-Dtemplate=<name>`**: Optional Facelet template name.

### `add-face-template`

Creates a new Facelet template.

- **`-Dname=<name>`**: Template name. (**Required**)
- **`-Dinserts=<list>`**: Comma-separated list of names for `ui:insert` elements.

### `add-forms-from-entities`

Generates Jakarta Faces/PrimeFaces CRUD forms from entities.

Run `add-domain-models` first. The generated forms and backing beans reference its domain models and repository interfaces.

- **`-Dforms-file=<path>`**: Path to the JSON form definition file. (**Required**)
- **`-Dentities-file=<path>`**: Path to the JSON entity definition file. (**Required**)

---

## Utilities

### `add-validation-api`

Adds Jakarta Validation API dependencies to enable annotation-based validation such as `@NotNull` and `@Size`.
