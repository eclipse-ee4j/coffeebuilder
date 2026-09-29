# Eclipse Coffee Builder Migration

## Objective

Migrate Coffee Builder from the personal GitHub organization into the Eclipse Foundation repository while preserving useful history, keeping changes incremental, and validating each migration phase independently.

Target repository:

```text
eclipse-ee4j/coffeebuilder
```

Target Java namespace and Maven groupId:

```text
org.eclipse.coffeebuilder
```

The migration must be performed incrementally and validated after each phase.

## Current source mapping

The monorepo was created from the following source branches:

- `coffee-builder-maven-plugin`
  - source branch: `main`
- `configuration`
  - source branch: `develop`
- `jakarta-ee-essentials`
  - source branch: `master`

The original histories were imported into the Eclipse repository using Git subtree without squashing.

## Current monorepo structure

```text
coffeebuilder/
├── coffee-builder-maven-plugin/
├── configuration/
├── jakarta-ee-minimal-archetype/
└── MIGRATION.md
```

## Migration rules

- Do not perform unrelated refactors.
- Do not change behavior unless explicitly requested by the active phase.
- Keep each migration phase independently buildable and testable.
- Do not commit or push unless explicitly requested.
- Report all modified, added, removed, or renamed files after each phase.
- Run all validation steps required by the active phase.
- Do not execute future phases early.
- Preserve migration history in this document where useful.

---

## Phase 1 — Java namespace

**Status: COMPLETE**

Scope:

- `coffee-builder-maven-plugin`

Change:

```text
io.github.coffeebuilder
→
org.eclipse.coffeebuilder
```

Included:

- Java package declarations
- imports
- static imports
- source directory paths
- test source directory paths
- Javadoc references
- Maven plugin configuration values that refer to Java packages

Excluded:

- Maven groupId
- artifactId
- version
- GitHub URLs
- website URLs
- configuration URLs
- unrelated Maven coordinates used by generated or integration-test projects

Validation result:

- `mvn test`
- 146 tests
- 0 failures
- 0 errors
- 0 skipped

Commit:

```text
3089cbc Migrate Java namespace to org.eclipse.coffeebuilder
```

---

## Phase 2 — Maven coordinates

**Status: COMPLETE**

Scope:

- `coffee-builder-maven-plugin`
- original `jakarta-ee-essentials` module
- documentation and examples directly tied to those Maven artifacts

Change Maven groupId:

```text
io.github.coffee-builder
→
org.eclipse.coffeebuilder
```

ArtifactIds were kept unchanged during this phase.

Included:

- project Maven groupId values
- README Maven invocation examples
- archetype invocation examples
- JavaDoc Maven plugin examples
- integration-test POM references to Coffee Builder artifacts
- tests that explicitly use Coffee Builder Maven coordinates

Excluded:

- unrelated generated/test application coordinates
- `io.github.coffeebuilder.it` used by the simple integration-test project
- versions
- GitHub URLs
- website URLs
- configuration URLs
- `distributionManagement`

Validation result:

- `coffee-builder-maven-plugin`: BUILD SUCCESS, 146 tests
- archetype module: BUILD SUCCESS

Commit:

```text
e0c1cac Migrate Maven coordinates to org.eclipse.coffeebuilder
```

---

## Phase 3 — Eclipse repository metadata

**Status: COMPLETE**

Scope:

- Maven metadata for `coffee-builder-maven-plugin`
- Maven metadata for the archetype module

Updated:

- top-level project URL:

```text
https://projects.eclipse.org/projects/ee4j.coffeebuilder
```

- developer organization:

```text
Eclipse Foundation
```

- organization URL:

```text
https://projects.eclipse.org/projects/ee4j.coffeebuilder
```

- SCM repository:

```text
https://github.com/eclipse-ee4j/coffeebuilder
```

SCM URLs remain GitHub repository URLs, while the Maven project URL points to the official Eclipse project page.

Excluded:

- `distributionManagement`
- configuration publishing strategy
- project versions
- CI/CD
- release automation
- copyright/license header migration

Validation result:

- `coffee-builder-maven-plugin`: BUILD SUCCESS, 146 tests
- archetype module: BUILD SUCCESS

Commit:

```text
eaf70f3 Update Eclipse project metadata
```

---

## Phase 4 — Configuration loading strategy

**Status: COMPLETE**

### Objective

Make the stable Coffee Builder configuration part of the Maven plugin artifact itself, while preserving the existing `-Ddevel=true` mechanism for testing configuration changes before a release.

### Production behavior

When the `devel` system property is absent or false:

- configuration JSON files are loaded from the plugin classpath
- configuration files are packaged in the plugin JAR under:

```text
/configuration/
```

- only JSON files from the monorepo `configuration/` directory are packaged
- the configuration `README` and `LICENSE` are not packaged as plugin resources
- no production HTTP request is required for configuration loading

This guarantees that the configuration used by a released plugin is the configuration shipped with that artifact.

### Development behavior

When:

```text
-Ddevel=true
```

is enabled, configuration is loaded remotely from:

```text
https://raw.githubusercontent.com/eclipse-ee4j/coffeebuilder/refs/heads/develop/configuration
```

This preserves the original purpose of development mode: testing catalog/configuration changes before they are included in a released artifact.

### Implementation

A package-private `ConfigurationLoader` chooses between:

- classpath resources for production/default mode
- remote HTTP resources for development mode

`HttpUtil` remains focused on HTTP responsibilities.

`CoffeeBuilderUtil` preserves its public configuration APIs.

The plugin POM packages JSON resources from:

```text
${project.basedir}/../configuration
```

into:

```text
configuration/
```

inside the plugin artifact.

### Validation result

- `mvn test`: BUILD SUCCESS
- 148 tests
- 0 failures
- 0 errors
- 0 skipped
- plugin packaging succeeded
- all eight expected `configuration/*.json` files were verified inside the built JAR
- packaged JSON files matched the monorepo source files

Commit:

```text
60587d0 Embed configuration resources in Maven plugin
```

---

## Phase 5 — Jakarta EE minimal archetype rename

**Status: COMPLETE**

### Objective

Rename the generic Jakarta EE archetype so its name accurately communicates its purpose: generating a minimal, clean, functional Jakarta EE project that can be used independently of Coffee Builder.

The archetype is not functionally coupled to the Coffee Builder Maven plugin.

### Directory rename

```text
jakarta-ee-essentials/
→
jakarta-ee-minimal-archetype/
```

### Maven artifact rename

```text
org.eclipse.coffeebuilder:jakarta-ee-essentials
→
org.eclipse.coffeebuilder:jakarta-ee-minimal-archetype
```

The Maven groupId remains:

```text
org.eclipse.coffeebuilder
```

The project version was intentionally left unchanged during this phase.

### Updated references

- module directory
- Maven `artifactId`
- README archetype invocation examples
- IDE `.iml` ignore entry
- current monorepo structure in this migration plan

### Preserved behavior

Generated/test project coordinates remain unchanged, including examples such as:

```text
com.example:testcustom:1.0.0
com.example:testejb10:1.0.0
com.example:testfull11:1.0.0
com.example:testweb10:1.0.0
```

### Validation result

`mvn verify` succeeded for the renamed archetype.

Validated scenarios:

- custom API path
- Jakarta EE 10 core / EJB
- Jakarta EE 11 full / web
- Jakarta EE 10 web profile

All generated projects compiled and packaged successfully.

Commit:

```text
9c96d33 Rename Jakarta EE archetype module
```

---

## Phase 6 — Monorepo build and unified versioning

**Status: COMPLETE**

### Objective

Turn the current repository into a coherent Maven monorepo with a single root build, shared project version, and explicit module relationship while keeping `configuration/` as a non-Maven resource component that participates in the Coffee Builder release lifecycle.

### Target structure

```text
coffeebuilder/
├── pom.xml
├── MIGRATION.md
├── coffee-builder-maven-plugin/
│   └── pom.xml
├── jakarta-ee-minimal-archetype/
│   └── pom.xml
└── configuration/
```

### Root Maven project

Create a root Maven parent/aggregator with:

```text
groupId:    org.eclipse.coffeebuilder
artifactId: coffeebuilder-parent
packaging:  pom
version:    0.1.0-SNAPSHOT
```

The root POM must declare exactly these Maven modules:

```text
coffee-builder-maven-plugin
jakarta-ee-minimal-archetype
```

The `configuration/` directory must **not** be declared as a Maven module.

### Unified versioning

Move the two Maven modules to the shared Coffee Builder development version:

```text
0.1.0-SNAPSHOT
```

Both child modules should inherit the project version from the root parent rather than declaring independent versions where practical.

The intended release model is:

```text
Coffee Builder 0.1.0
├── coffee-builder-maven-plugin 0.1.0
├── jakarta-ee-minimal-archetype 0.1.0
└── configuration/ from the same repository state/tag
```

`configuration/` has no Maven artifact version of its own; its version is represented by the repository release/tag that also produces the Maven artifacts.

### Parent relationship

Both Maven modules should declare the root project as their parent using:

```text
org.eclipse.coffeebuilder:coffeebuilder-parent:0.1.0-SNAPSHOT
```

Use an appropriate relative path to the root POM.

Child artifactIds remain:

```text
coffee-builder-maven-plugin
jakarta-ee-minimal-archetype
```

### Metadata inheritance

Prefer inheritance from the root POM for project metadata that is truly common and safe to centralize, such as:

- `groupId`
- project version
- project URL
- license
- SCM repository
- organization metadata
- source encoding

Do not aggressively centralize unrelated build configuration merely to reduce duplication.

Module-specific metadata and build behavior should remain in the module POMs.

### Configuration relationship

Preserve the Phase 4 behavior where the Maven plugin packages JSON files from:

```text
configuration/
```

The root build must continue to allow the Maven plugin to access and package those JSON resources correctly.

Do not convert `configuration/` into a Maven artifact or Maven module.

### Version references in documentation/tests

Update references to the old Coffee Builder artifact version where they refer to the actual Coffee Builder Maven artifacts.

For example, README invocations that currently use:

```text
0.0.8
```

should be updated to the new development/release context where appropriate.

Do **not** change unrelated generated-project versions such as:

```text
1.0.0
1.0-SNAPSHOT
```

when those versions belong to projects generated by the archetype or integration-test fixtures rather than Coffee Builder itself.

### Validation

From the repository root, run:

```bash
mvn verify
```

The root reactor build must include both Maven modules and complete successfully.

Also verify:

- the Maven plugin unit tests pass
- the archetype integration tests pass
- the Maven plugin JAR still contains all expected `configuration/*.json` resources
- child module versions resolve to `0.1.0-SNAPSHOT`
- the reactor order is valid
- no Coffee Builder Maven artifact remains on `0.0.8-SNAPSHOT`
- no accidental changes were made to generated/test-project versions

### Validation result

- root `mvn verify`: BUILD SUCCESS
- reactor order: parent, Maven plugin, minimal archetype
- Maven plugin: BUILD SUCCESS, 148 tests, 0 failures, 0 errors, 0 skipped
- minimal archetype: BUILD SUCCESS
- all four archetype integration scenarios passed
- both child modules resolved to `0.1.0-SNAPSHOT`
- the plugin JAR contained all eight expected `configuration/*.json` resources
- packaged configuration JSON matched the monorepo source files
- generated/test-project versions remained unchanged

### Explicit exclusions

Do not change in Phase 6:

- CI/CD workflows
- GitHub Actions behavior
- release signing
- deployment credentials
- `distributionManagement`
- Maven Central publishing strategy
- Eclipse release automation
- copyright/license headers
- line-ending policy
- `.gitattributes`
- branch strategy
- repository protection rules
- unrelated source code behavior

### Completion report

When Phase 6 is executed, report:

- root POM created
- all child POM changes
- inherited vs module-specific metadata decisions
- all Coffee Builder version changes
- documentation version changes
- complete Maven reactor order
- complete `mvn verify` result
- plugin test result
- archetype integration-test result
- confirmation of packaged configuration resources
- remaining `0.0.8` references and why they remain
- any design decisions or problems encountered

Do not commit or push automatically.

---

## Phase 7 — Monorepo CI baseline

**Status: COMPLETE**

### Objective

Replace the obsolete module-local CI/CD workflows with one repository-root CI workflow that validates the complete Maven reactor without performing deployment or release operations.

### Branch strategy

- `develop` is the initial official development branch.
- CI runs for pull requests targeting `develop`.
- CI runs for pushes to `develop`.
- CI can also be started manually with `workflow_dispatch`.
- `main` does not exist yet and will be created later for the first stable/incubating release.
- No legacy default branch is used or referenced by the workflow.

### Root workflow

Created:

```text
.github/workflows/ci.yml
```

The workflow uses:

- `ubuntu-latest`
- Java 21
- Eclipse Temurin
- Maven dependency caching

The complete monorepo is built from the repository root with:

```bash
mvn --batch-mode --no-transfer-progress verify
```

The root reactor validates:

- `coffeebuilder-parent`
- `coffee-builder-maven-plugin`
- `jakarta-ee-minimal-archetype`

### Obsolete workflows removed

Removed:

```text
coffee-builder-maven-plugin/.github/workflows/maven-ci-cd.yml
jakarta-ee-minimal-archetype/.github/workflows/maven-ci-cd.yml
```

These module-local workflows contained obsolete snapshot deployment, release, tagging, signing, credential, and branch behavior and were not appropriate for the monorepo baseline.

### Explicit exclusions

This phase does not add or perform:

- Maven Central deployment
- snapshot deployment
- release creation
- Git tagging
- GPG signing
- deployment credentials or Sonatype secrets
- branch triggers other than `develop`
- release or publishing preparation

### Validation result

- root workflow structure: validated
- root Maven command: `mvn verify`
- reactor result: BUILD SUCCESS
- the parent, Maven plugin, and minimal archetype modules all completed successfully
- no deployment or release behavior remains in the active workflow

Do not commit or push automatically.

---

## Future phases

- Phase 8 — Maven publishing and Eclipse release preparation
- Phase 9 — Repository hygiene, legal metadata, line endings, and contributor documentation
- Phase 10 — Documentation and JakartaOne readiness
