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

Do not change yet:

- Maven groupId `io.github.coffee-builder`
- artifactId
- version
- GitHub URLs
- website URLs
- `PRD_BASE_URL`
- `DEV_BASE_URL`
- README Maven invocation examples
- configuration files
- `jakarta-ee-essentials`
- unrelated sample/test Maven groupIds

Validation:

```bash
mvn test
```

from:

```text
coffee-builder-maven-plugin
```

## Future phases

- Phase 2 — Maven coordinates
- Phase 3 — Eclipse repository metadata
- Phase 4 — Configuration URL strategy
- Phase 5 — Jakarta EE Essentials migration
- Phase 6 — Monorepo parent/build structure
- Phase 7 — CI/CD and Eclipse release preparation
- Phase 8 — Documentation and JakartaOne readiness
