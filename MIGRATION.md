# Eclipse Coffee Builder Migration

## Objective

Migrate Coffee Builder from the personal GitHub organization into:

- Repository: `eclipse-ee4j/coffeebuilder`
- Java namespace: `org.eclipse.coffeebuilder`
- Maven groupId: `org.eclipse.coffeebuilder`

The migration must be performed incrementally and validated after each phase.

## Current source mapping

- `coffee-builder-maven-plugin`
  - source branch: `main`
- `configuration`
  - source branch: `develop`
- `jakarta-ee-essentials`
  - source branch: `master`

## Monorepo structure

```text
coffeebuilder/
├── coffee-builder-maven-plugin/
├── configuration/
└── jakarta-ee-essentials/
```

## Migration rules

- Do not perform unrelated refactors.
- Do not change behavior unless explicitly requested.
- Keep each migration phase independently buildable/testable.
- Do not commit or push unless explicitly requested.
- Report all modified files after each phase.
- Run relevant tests after each phase.

## Phase 1 — Java namespace — COMPLETED

Scope: `coffee-builder-maven-plugin`.

Changed:

```text
io.github.coffeebuilder
→
org.eclipse.coffeebuilder
```

Included Java package declarations, imports, static imports, source paths, test paths, Javadoc references, and Maven plugin configuration referring to Java packages.

Validation completed successfully with 146 tests, 0 failures, 0 errors, 0 skipped.

## Phase 2 — Maven coordinates — COMPLETED

Scope:

- `coffee-builder-maven-plugin`
- `jakarta-ee-essentials`
- Documentation/examples directly tied to those Maven artifacts

Changed Maven groupId:

```text
io.github.coffee-builder
→
org.eclipse.coffeebuilder
```

ArtifactIds and versions remained unchanged.

Validation completed successfully for both Maven projects.

## Phase 3 — Eclipse repository metadata — COMPLETED

Scope:

- Root repository metadata where applicable
- `coffee-builder-maven-plugin` metadata
- `jakarta-ee-essentials` metadata

Updated project metadata to the Eclipse Coffee Builder project and monorepo.

Canonical project page:

```text
https://projects.eclipse.org/projects/ee4j.coffeebuilder
```

Canonical source repository:

```text
https://github.com/eclipse-ee4j/coffeebuilder
```

SCM entries point to the Eclipse GitHub monorepo.

The configuration runtime URLs were intentionally excluded from this phase.

## Phase 4 — Configuration loading strategy

### Objective

Make the stable Coffee Builder configuration part of the Maven plugin artifact itself, while preserving the existing `-Ddevel=true` mechanism for testing configuration changes before a release.

### Production behavior

When the `devel` system property is absent or false:

- Load configuration JSON files from the plugin classpath.
- Configuration files must be packaged inside the plugin JAR under:

```text
/configuration/
```

- The packaged files must come from the monorepo root `configuration/` directory.
- Only JSON configuration files should be packaged from that directory.
- Do not package the configuration repository README or LICENSE as plugin resources.

This guarantees that the configuration used in production corresponds exactly to the released plugin artifact and does not change remotely after publication.

### Development behavior

When:

```text
-Ddevel=true
```

is enabled:

- Keep loading configuration remotely through HTTP.
- Use the Eclipse Coffee Builder monorepo development branch:

```text
https://raw.githubusercontent.com/eclipse-ee4j/coffeebuilder/refs/heads/develop/configuration
```

- Preserve the existing purpose of development mode: testing configuration changes before they are included in a released Coffee Builder artifact.

### Implementation guidelines

- Keep `HttpUtil` focused on HTTP operations.
- Introduce a configuration-loading abstraction responsible for choosing between:
  - classpath resources for production
  - remote HTTP resources for development
- Avoid duplicating JSON parsing logic.
- Preserve the existing configuration API exposed by `CoffeeBuilderUtil` where practical.
- Do not introduce plugin-version discovery or tag-based URL construction.
- Do not introduce unrelated refactors.

### Tests

Add or update tests covering at least:

- production/default mode reads configuration from the classpath
- `devel=false` reads configuration from the classpath
- `devel=true` selects remote development configuration
- packaged configuration resources are available to the plugin
- existing configuration-related behavior remains compatible

### Validation

Run:

```bash
mvn test
```

for `coffee-builder-maven-plugin`.

Also build the plugin and verify that the resulting JAR contains the expected JSON files under:

```text
configuration/
```

### Do not change yet

- project versions
- artifactIds
- `distributionManagement`
- CI/CD or release workflows
- Jakarta EE Essentials behavior
- root monorepo build structure
- unrelated application generation behavior

Do not commit or push automatically.

## Future phases

- Phase 5 — Jakarta EE Essentials migration
- Phase 6 — Monorepo parent/build structure
- Phase 7 — CI/CD and Eclipse release preparation
- Phase 8 — Documentation and JakartaOne readiness
