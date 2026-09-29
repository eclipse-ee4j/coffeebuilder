# Eclipse Coffee Builder Migration

## Objective

Migrate Coffee Builder from the personal GitHub organization into the Eclipse Foundation repository while preserving behavior, history, and release coherence.

Target repository:

- `https://github.com/eclipse-ee4j/coffeebuilder`

Target Java namespace and Maven groupId:

- `org.eclipse.coffeebuilder`

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
└── jakarta-ee-minimal-archetype/
```

## Migration rules

- Do not perform unrelated refactors.
- Do not change behavior unless explicitly requested by the current phase.
- Keep each migration phase independently buildable/testable.
- Do not commit or push unless explicitly requested.
- Report all modified files after each phase.
- Run relevant tests after each phase.

## Phase 1 — Java namespace

Status: completed.

Scope:

- `coffee-builder-maven-plugin`

Change:

```text
io.github.coffeebuilder
→
org.eclipse.coffeebuilder
```

Includes:

- Java package declarations
- imports
- static imports
- source directory paths
- test source directory paths
- Javadoc references
- Maven plugin configuration that references Java packages

Excluded from this phase:

- Maven groupId `io.github.coffee-builder`
- artifactId
- version
- GitHub URLs
- website URLs
- `PRD_BASE_URL`
- `DEV_BASE_URL`
- README Maven invocation examples
- configuration files
- jakarta-ee-essentials
- unrelated sample/test Maven groupIds

Validation completed:

```text
mvn test
```

Result:

- 146 tests
- 0 failures
- 0 errors
- 0 skipped

## Phase 2 — Maven coordinates

Status: completed.

Scope:

- `coffee-builder-maven-plugin`
- `jakarta-ee-essentials`
- documentation/examples directly tied to those Maven artifacts

Change Maven groupId:

```text
io.github.coffee-builder
→
org.eclipse.coffeebuilder
```

ArtifactIds remained unchanged during this phase:

```text
coffee-builder-maven-plugin
jakarta-ee-essentials
```

Includes:

- project Maven `groupId`
- Maven invocation examples in README files
- archetype invocation examples
- Maven plugin examples in JavaDoc or sample POMs
- tests that explicitly validate or use Coffee Builder Maven coordinates
- integration-test POMs when they refer to Coffee Builder artifacts

Excluded from this phase:

- Java namespace
- project version
- GitHub URLs
- website URLs
- `PRD_BASE_URL`
- `DEV_BASE_URL`
- `distributionManagement`
- repository structure
- `configuration`
- unrelated sample application groupIds

Validation completed:

- `coffee-builder-maven-plugin`: BUILD SUCCESS, 146 tests, 0 failures, 0 errors, 0 skipped
- `jakarta-ee-essentials`: BUILD SUCCESS

## Phase 3 — Eclipse repository metadata

Status: completed.

Scope:

- root repository metadata where applicable
- `coffee-builder-maven-plugin` metadata
- `jakarta-ee-essentials` metadata

Updated:

- project URL to `https://projects.eclipse.org/projects/ee4j.coffeebuilder`
- SCM URLs to `https://github.com/eclipse-ee4j/coffeebuilder`
- developer organization to Eclipse Foundation
- organization URL to the Eclipse project page

Excluded from this phase:

- `distributionManagement`
- `PRD_BASE_URL`
- `DEV_BASE_URL`
- configuration publishing strategy
- versions
- artifactIds
- license text or copyright headers
- CI/CD
- release automation

Validation completed:

- `coffee-builder-maven-plugin`: BUILD SUCCESS, 146 tests, 0 failures, 0 errors, 0 skipped
- `jakarta-ee-essentials`: BUILD SUCCESS

## Phase 4 — Configuration loading strategy

Status: completed.

### Objective

Make the stable Coffee Builder configuration part of the Maven plugin artifact itself while preserving the existing `-Ddevel=true` mechanism for testing configuration changes before a release.

### Production behavior

When the `devel` system property is absent or false:

- load configuration JSON files from the plugin classpath
- package configuration files inside the plugin JAR under:

```text
/configuration/
```

- source the packaged files directly from the monorepo root `configuration/` directory
- package only JSON configuration files
- do not package the configuration README or LICENSE into the plugin JAR

This guarantees that the production configuration corresponds exactly to the released plugin artifact and does not change remotely after publication.

### Development behavior

When:

```text
-Ddevel=true
```

is enabled:

- load configuration remotely through HTTP
- use the Eclipse Coffee Builder monorepo development branch:

```text
https://raw.githubusercontent.com/eclipse-ee4j/coffeebuilder/refs/heads/develop/configuration
```

### Implementation

- introduced an internal `ConfigurationLoader`
- kept `HttpUtil` focused on HTTP operations
- preserved the public `CoffeeBuilderUtil` API
- removed the obsolete production remote URL strategy
- packaged the eight configuration JSON files from `../configuration`

Validation completed:

- `mvn test`: BUILD SUCCESS
- 148 tests
- 0 failures
- 0 errors
- 0 skipped
- plugin JAR contains all eight expected `configuration/*.json` resources

## Phase 5 — Rename and validate Jakarta EE minimal archetype

Status: completed.

### Objective

Rename the generic Jakarta EE starter archetype so that its purpose is explicit and independent from Coffee Builder.

The archetype generates a minimal, clean, functional Jakarta EE project and can be used independently of the Coffee Builder Maven plugin.

Rename:

```text
jakarta-ee-essentials
→
jakarta-ee-minimal-archetype
```

### Scope

Rename the module directory:

```text
jakarta-ee-essentials/
→
jakarta-ee-minimal-archetype/
```

Change the Maven artifactId:

```text
org.eclipse.coffeebuilder:jakarta-ee-essentials
→
org.eclipse.coffeebuilder:jakarta-ee-minimal-archetype
```

Update references that directly identify the archetype artifact, including:

- module `pom.xml`
- README Maven archetype invocation examples
- `.gitignore` entries tied to the old module/artifact name
- tests or integration-test metadata that directly refer to the archetype artifactId
- migration documentation references to the module name

### Preserve

Do not change the conceptual purpose of the archetype.

It must remain:

- independent from the Coffee Builder Maven plugin
- usable as a standalone Maven archetype
- minimal and intentionally opinion-light
- compatible with the existing supported Jakarta EE profiles/modules/versions

### Do not change yet

- project version `0.0.8-SNAPSHOT`
- generated sample project versions such as `1.0.0` or `1.0-SNAPSHOT`
- Maven groupId `org.eclipse.coffeebuilder`
- generated project package conventions
- archetype functional behavior
- `distributionManagement`
- CI/CD workflows
- release automation
- root parent/aggregator structure

### Validation

From the renamed archetype module, run:

```text
mvn verify
```

The integration scenarios must continue to succeed, including the existing archetype IT cases for:

- custom API path
- Jakarta EE 10 core / EJB
- Jakarta EE 11 full / web
- Jakarta EE 10 web profile

No functional behavior change is expected in this phase.

Validation completed:

- `mvn verify`: BUILD SUCCESS
- custom API path: BUILD SUCCESS
- Jakarta EE 10 core / EJB: BUILD SUCCESS
- Jakarta EE 11 full / web: BUILD SUCCESS
- Jakarta EE 10 web profile: BUILD SUCCESS

Do not commit or push automatically.

## Future phases

- Phase 6 — Monorepo parent/build structure and coordinated versioning
- Phase 7 — CI/CD and Eclipse release preparation
- Phase 8 — repository hygiene, line endings, legal headers and contribution metadata
- Phase 9 — documentation and JakartaOne readiness
