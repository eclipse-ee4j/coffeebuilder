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

## Phase 1 — Java namespace

Status: completed.

Scope:

`coffee-builder-maven-plugin`

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

Do not change in this phase:

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

Validation:

```bash
mvn test
```

from:

```text
coffee-builder-maven-plugin
```

Result:

- 146 tests
- 0 failures
- 0 errors
- 0 skipped

## Phase 2 — Maven coordinates

Scope:

- `coffee-builder-maven-plugin`
- `jakarta-ee-essentials`
- Documentation/examples directly tied to those Maven artifacts

Change Maven groupId:

```text
io.github.coffee-builder
→
org.eclipse.coffeebuilder
```

Keep artifactIds unchanged:

```text
coffee-builder-maven-plugin
jakarta-ee-essentials
```

Includes:

- Project Maven `groupId`
- Maven invocation examples in README files
- Archetype invocation examples
- Maven plugin examples in JavaDoc or sample POMs
- Tests that explicitly validate or use the Coffee Builder Maven coordinates
- Integration-test POMs when they refer to Coffee Builder artifacts

Do not change yet:

- Java namespace (`org.eclipse.coffeebuilder` is already migrated)
- Project version
- GitHub URLs
- Website URLs
- `PRD_BASE_URL`
- `DEV_BASE_URL`
- `distributionManagement`
- Repository structure
- `configuration`
- Unrelated sample application groupIds that merely identify generated/test projects

Validation:

For `coffee-builder-maven-plugin`:

```bash
mvn test
```

For `jakarta-ee-essentials`:

```bash
mvn test
```

Do not commit or push automatically.

## Phase 3 — Eclipse repository metadata

Scope:
- Root repository metadata
- coffee-builder-maven-plugin metadata
- jakarta-ee-essentials metadata

Update:
- Project URLs to eclipse-ee4j/coffeebuilder where appropriate
- SCM URLs to eclipse-ee4j/coffeebuilder
- Organization name/URL to Eclipse Foundation / project page where appropriate
- Repository references in README files where appropriate

Do not change yet:
- distributionManagement
- PRD_BASE_URL / DEV_BASE_URL
- configuration publishing strategy
- versions
- artifactIds
- license text or copyright headers
- CI/CD
- release automation

Validation:
- Maven builds must still pass
- No stale repository URLs should remain in scoped files, except historical references in MIGRATION.md

Do not commit or push automatically.

## Future phases
 
- Phase 4 — Configuration URL strategy
- Phase 5 — Jakarta EE Essentials migration
- Phase 6 — Monorepo parent/build structure
- Phase 7 — CI/CD and Eclipse release preparation
- Phase 8 — Documentation and JakartaOne readiness
