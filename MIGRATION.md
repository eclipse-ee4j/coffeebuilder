# Eclipse Coffee Builder Migration

## Objective

Migrate Coffee Builder from the personal GitHub organization into the
Eclipse Foundation repository while preserving project history,
behavior, and a clear audit trail.

-   Repository: `eclipse-ee4j/coffeebuilder`
-   Java namespace: `org.eclipse.coffeebuilder`
-   Maven groupId: `org.eclipse.coffeebuilder`
-   Current unified development version: `0.1.0-SNAPSHOT`
-   Initial official development branch: `develop`

The migration must be performed incrementally and validated after each
phase.

## Current source mapping

The initial monorepo import preserved the histories of:

-   `coffee-builder-maven-plugin`
    -   source branch: `main`
-   `configuration`
    -   source branch: `develop`
-   `jakarta-ee-essentials`
    -   source branch: `master`
    -   renamed during migration to `jakarta-ee-minimal-archetype`

## Current monorepo structure

``` text
coffeebuilder/
├── .github/
│   └── workflows/
│       └── ci.yml
├── coffee-builder-maven-plugin/
├── configuration/
├── jakarta-ee-minimal-archetype/
├── MIGRATION.md
└── pom.xml
```

## Migration rules

-   Do not perform unrelated refactors.
-   Do not change behavior unless explicitly requested by a phase.
-   Keep each migration phase independently buildable/testable.
-   Do not commit or push unless explicitly requested.
-   Report modified, added, removed, and renamed files after each phase.
-   Run the relevant tests after each phase.
-   Preserve migration history in this document.
-   Do not rewrite historical phase descriptions merely to match the
    current state.

------------------------------------------------------------------------

## Phase 1 --- Java namespace

**Status: COMPLETE**

Scope: `coffee-builder-maven-plugin`.

Migrated:

``` text
io.github.coffeebuilder
→
org.eclipse.coffeebuilder
```

Included package declarations, imports, static imports, source/test
directory paths, Javadoc references, and Maven plugin configuration that
referenced the Java namespace.

Maven coordinates, repository URLs, configuration URLs, and unrelated
test Maven groupIds were intentionally left unchanged in this phase.

Validation:

-   `mvn test`: BUILD SUCCESS
-   146 tests, 0 failures, 0 errors, 0 skipped
-   No active old Java namespace remained except the intentionally
    unrelated integration-test project groupId.

------------------------------------------------------------------------

## Phase 2 --- Maven coordinates

**Status: COMPLETE**

Migrated Coffee Builder Maven coordinates:

``` text
io.github.coffee-builder
→
org.eclipse.coffeebuilder
```

Applied to:

-   `coffee-builder-maven-plugin`
-   the archetype then named `jakarta-ee-essentials`
-   Maven invocation examples
-   relevant JavaDoc/plugin examples
-   Coffee Builder coordinate-based tests and fixtures

ArtifactIds and project versions were intentionally unchanged in this
phase.

Validation:

-   Maven plugin: BUILD SUCCESS, 146 tests
-   Archetype: BUILD SUCCESS

------------------------------------------------------------------------

## Phase 3 --- Eclipse repository metadata

**Status: COMPLETE**

Updated canonical project metadata to Eclipse Foundation resources.

Project URL:

``` text
https://projects.eclipse.org/projects/ee4j.coffeebuilder
```

Canonical SCM:

``` text
https://github.com/eclipse-ee4j/coffeebuilder
```

Updated project organization metadata to Eclipse Foundation where
appropriate.

Intentionally excluded:

-   `distributionManagement`
-   configuration loading URLs
-   release automation
-   legal/copyright header migration

Validation:

-   Maven plugin: BUILD SUCCESS
-   Archetype: BUILD SUCCESS

------------------------------------------------------------------------

## Phase 4 --- Configuration loading strategy

**Status: COMPLETE**

### Objective

Make stable Coffee Builder configuration part of the Maven plugin
artifact while preserving `-Ddevel=true` for testing configuration
changes before release.

### Production/default behavior

When `devel` is absent or false:

-   Configuration is loaded from the plugin classpath.
-   JSON files are packaged under:

``` text
/configuration/
```

-   Files originate from the monorepo root `configuration/` directory.
-   Only JSON files are packaged; `README` and `LICENSE` are excluded.

### Development behavior

When:

``` text
-Ddevel=true
```

configuration is loaded remotely from:

``` text
https://raw.githubusercontent.com/eclipse-ee4j/coffeebuilder/refs/heads/develop/configuration
```

### Implementation

Introduced an internal `ConfigurationLoader`.

Responsibilities:

``` text
ConfigurationLoader
├── default / devel=false → classpath resources
└── devel=true            → HttpUtil → remote development configuration
```

`HttpUtil` remains focused on HTTP.

The public `CoffeeBuilderUtil` API was preserved.

Validation:

-   148 tests, 0 failures, 0 errors, 0 skipped
-   plugin package build succeeded
-   all eight expected configuration JSON files were present in the JAR
-   packaged JSON content matched the monorepo source files

------------------------------------------------------------------------

## Phase 5 --- Jakarta EE minimal archetype rename

**Status: COMPLETE**

Renamed:

``` text
jakarta-ee-essentials
→
jakarta-ee-minimal-archetype
```

Maven artifact:

``` text
org.eclipse.coffeebuilder:jakarta-ee-essentials
→
org.eclipse.coffeebuilder:jakarta-ee-minimal-archetype
```

Rationale:

The archetype is independently useful and creates a minimal, clean,
functional Jakarta EE project. It is not coupled to Coffee Builder and
therefore should not be named as a Coffee Builder-specific archetype.

Unrelated generated-project coordinates and package conventions remained
unchanged.

Validation:

-   `mvn verify`: BUILD SUCCESS
-   custom API path scenario: success
-   Jakarta EE 10 core/EJB: success
-   Jakarta EE 11 full/web: success
-   Jakarta EE 10 web profile: success

------------------------------------------------------------------------

## Phase 6 --- Monorepo build and unified versioning

**Status: COMPLETE**

### Objective

Turn the imported repositories into a coherent Maven multi-module build
with one shared Coffee Builder development version.

Created root parent/aggregator:

``` text
org.eclipse.coffeebuilder:coffeebuilder-parent:0.1.0-SNAPSHOT
```

Root Maven modules:

``` text
coffee-builder-maven-plugin
jakarta-ee-minimal-archetype
```

`configuration/` is intentionally not a Maven module.

Both Maven modules inherit the Coffee Builder groupId and version from
the parent where appropriate.

Effective coordinates:

``` text
org.eclipse.coffeebuilder:coffee-builder-maven-plugin:0.1.0-SNAPSHOT
org.eclipse.coffeebuilder:jakarta-ee-minimal-archetype:0.1.0-SNAPSHOT
```

The shared `0.1.0-SNAPSHOT` version establishes the first
Eclipse-incubation development line while preserving the historical
`0.0.x` record.

Validation from repository root:

``` bash
mvn verify
```

Result:

-   parent: SUCCESS
-   Maven plugin: SUCCESS
-   minimal archetype: SUCCESS
-   plugin tests: 148, all passing
-   archetype integration scenarios: all passing
-   configuration resources remained correctly packaged

Publishing, signing, CI/CD, and `distributionManagement` were
intentionally not centralized in this phase.

------------------------------------------------------------------------

## Phase 7 --- Monorepo CI baseline

**Status: COMPLETE**

### Objective

Replace obsolete module-local CI/CD workflows with one repository-root
CI workflow that validates the complete Maven reactor without performing
deployment or release operations.

### Branch strategy

-   `develop` is the initial official development branch.
-   CI runs for pull requests targeting `develop`.
-   CI runs for pushes to `develop`.
-   CI can be started manually with `workflow_dispatch`.
-   `main` does not exist yet.
-   `main` will be created later when the project is ready for its first
    stable/incubating release.
-   `master` is not used.

### Root workflow

Created:

``` text
.github/workflows/ci.yml
```

The workflow uses:

-   `ubuntu-latest`
-   Java 21
-   Eclipse Temurin
-   Maven dependency caching
-   read-only repository contents permission

Build command:

``` bash
mvn --batch-mode --no-transfer-progress verify
```

The root reactor validates:

1.  `coffeebuilder-parent`
2.  `coffee-builder-maven-plugin`
3.  `jakarta-ee-minimal-archetype`

Removed obsolete module-local workflows:

``` text
coffee-builder-maven-plugin/.github/workflows/maven-ci-cd.yml
jakarta-ee-minimal-archetype/.github/workflows/maven-ci-cd.yml
```

No active workflow performs Maven Central deployment, snapshot
deployment, release creation, Git tagging, GPG signing, or
credential-based publication.

Validation:

-   workflow structure validated
-   root reactor: BUILD SUCCESS
-   plugin: 148 tests, all passing
-   all four archetype integration scenarios passed

------------------------------------------------------------------------

## Phase 8 --- Repository hygiene, legal metadata, line endings, and contributor documentation

**Status: COMPLETE**

### Objective

Prepare the monorepo as an Eclipse Foundation project repository before
publication/release work begins.

This phase must prioritize the current Eclipse Foundation
legal-documentation requirements and templates. Do not invent or
paraphrase legal text where an Eclipse Foundation template or canonical
document is required.

### Root repository documentation

Create or establish the required repository-root documentation, using
current Eclipse Foundation guidance and canonical templates where
applicable:

``` text
README.md
LICENSE
CONTRIBUTING.md
SECURITY.md
CODE_OF_CONDUCT.md
```

Requirements:

-   `README.md` must identify Eclipse Coffee Builder, explain its
    purpose at a high level, identify the Maven plugin and minimal
    archetype, point to the official Eclipse project page and canonical
    GitHub repository, and provide a minimal build command.
-   `CONTRIBUTING.md` must explain the active `develop` branch, how to
    build the full reactor, how to submit pull requests, the Eclipse
    Contributor Agreement requirement, and point to the Eclipse
    Foundation Terms of Use, project/developer resources, and canonical
    repository.
-   `SECURITY.md` must follow current Eclipse Foundation security
    guidance/template and clearly identify the supported
    security-reporting channel. Do not invent a private reporting
    mechanism that has not been provisioned.
-   `CODE_OF_CONDUCT.md` must use or point to the current Eclipse
    Foundation Community Code of Conduct as required by current Eclipse
    guidance.
-   `LICENSE` must reflect the project license already approved/declared
    for Eclipse Coffee Builder. Do not silently change the project
    license during this phase.

### License and notice audit

Audit the existing module-level `LICENSE` files and any notice/legal
files.

Determine:

-   whether module-level copies should remain in distributed artifacts;
-   whether a root `NOTICE` or module/distribution notice file is
    required by current Eclipse guidance;
-   whether any existing legal file conflicts with the project metadata.

Do not delete legal files merely to reduce duplication.

### Copyright and license headers

Audit source/project-content headers across:

-   Java source and tests
-   Groovy scripts
-   shell/PowerShell/batch scripts where technically feasible
-   relevant configuration/source files where technically feasible

Current personal headers such as:

``` text
Copyright 2024 Diego Silva ...
```

must not be bulk-rewritten by assumption.

Instead:

1.  inventory current header patterns;
2.  compare them with current Eclipse Foundation legal-documentation
    guidance;
3.  apply only a legally appropriate, consistent form;
4.  preserve legitimate historical copyright ownership;
5.  do not replace an original copyright owner with "Eclipse Foundation"
    merely because the project moved to Eclipse.

If the correct transformation is ambiguous, report it and leave the
affected headers unchanged for EMO/IP confirmation rather than inventing
legal text.

### Maven legal metadata

Audit the root and child POMs for current Eclipse/Maven metadata
expectations.

At minimum verify:

-   project license metadata
-   SPDX-compatible license representation where appropriate/currently
    supported
-   project name and description
-   organization
-   SCM
-   issue-management metadata if a canonical issue tracker is available
-   developer metadata
-   source encoding

Do not change publishing or `distributionManagement` in this phase.

### Line endings and repository text policy

Create a root `.gitattributes` that establishes predictable
cross-platform text handling.

Goals:

-   normalize repository text files in Git;
-   use LF for repository text content unless a file type has a strong
    platform-specific reason otherwise;
-   preserve appropriate behavior for Windows scripts such as
    `.cmd`/`.bat` when required;
-   mark binary formats appropriately when useful;
-   eliminate recurring accidental LF/CRLF churn.

Do not mass-reformat unrelated source code.

If normalization changes tracked files, isolate and report those changes
clearly.

### Root `.gitignore`

Create or consolidate a root `.gitignore` appropriate for the Maven
monorepo.

It should cover generated/build/editor artifacts without hiding source
files or migration/project metadata.

Review existing module `.gitignore` files before deciding whether they
remain necessary.

### Repository hygiene audit

Search for stale references to the former personal project
infrastructure, including:

``` text
github.com/coffee-builder
coffee-builder.github.io
io.github.coffee-builder
io.github.coffeebuilder
jakarta-ee-essentials
```

Historical references inside `MIGRATION.md` may remain intentionally.

Do not remove historical Git information.

### Validation

After Phase 8:

1.  Run:

``` bash
mvn verify
```

from the repository root.

2.  Run repository-wide searches for stale active identity/URL
    references.

3.  Run `git diff --check`.

4.  Verify line-ending policy does not corrupt Maven wrapper scripts,
    Windows scripts, XML, JSON, YAML, Markdown, or Java source.

5.  Report any legal/header decision that still requires Eclipse EMO/IP
    confirmation.

### Explicit exclusions

Do not perform:

-   Maven Central publication
-   snapshot deployment
-   release publication
-   GPG signing
-   release/tag creation
-   `distributionManagement` redesign
-   branch creation
-   GitHub branch-protection configuration
-   project-version changes
-   unrelated code refactors
-   JakartaOne/demo documentation work

### Completion report

When Phase 8 is executed, report:

-   every root documentation/legal file added or changed;
-   legal templates/sources followed;
-   module-level license/notice decisions;
-   copyright/header inventory and changes;
-   any legal items deliberately left pending for EMO/IP confirmation;
-   `.gitattributes` policy;
-   `.gitignore` decisions;
-   stale-reference audit result;
-   POM metadata changes;
-   complete `mvn verify` result;
-   `git diff --check` result;
-   line-ending validation result.

### Execution result

Root repository files added:

``` text
README.md
LICENSE
NOTICE.md
CONTRIBUTING.md
SECURITY.md
CODE_OF_CONDUCT.md
.gitattributes
.gitignore
```

Legal and contributor documentation followed the current Eclipse
Project Handbook legal-documentation and contributor-guide requirements,
the Eclipse Security Team `SECURITY.md` template and baseline security
policy, and the canonical Eclipse Foundation Community Code of Conduct.
The project page confirms Apache License 2.0 as the declared project
license; the root `LICENSE` contains the canonical Apache License 2.0
text and matches the existing module license files after line-ending
normalization.

The existing module-level `LICENSE` files remain in place. The Maven
plugin and minimal archetype builds now package their module license and
the root notice as:

``` text
META-INF/LICENSE
META-INF/NOTICE.md
```

The copyright/header audit covered Java production and test sources,
archetype Java templates, Groovy scripts, Maven wrapper scripts, XML,
JSON, YAML, HTML, FreeMarker, and Mustache content. The Java inventory
was:

-   46 Maven plugin production Java files: 40 with existing personal
    Apache License headers and 6 without copyright headers;
-   27 Maven plugin test Java files without copyright headers;
-   2 generated-project Java templates without copyright headers;
-   3 Groovy scripts without project copyright headers.

Existing personal headers use several historical forms, including
`Diego Silva`, `Diego Silva` with an email address, and `dsilva`.
Apache Software Foundation headers in Maven wrapper and archetype
metadata content were preserved. No source copyright or license header
was changed because the proper owner name, year treatment, and treatment
of currently unheadered files require EMO/IP confirmation. In
particular, no ownership was assigned to the Eclipse Foundation.

Maven metadata changes:

-   added the canonical GitHub issue tracker to the root POM for child
    inheritance;
-   added `repo` distribution metadata to the existing Apache License
    declaration;
-   made both child project names human-readable;
-   added legal-resource packaging to both child builds;
-   retained existing organization, SCM, developer, source-encoding,
    publishing, and `distributionManagement` configuration unchanged.

The root `.gitattributes` normalizes text to LF, retains CRLF for
Windows `.cmd` and `.bat` scripts, and marks common archives, images,
and other binary formats as binary. No mass normalization was performed.
Byte-level checks confirmed the new text files use LF and both Maven
wrapper `.cmd` files retain CRLF without mixed line endings.

The root `.gitignore` covers Maven output, Java crash/build files,
common IDE/editor metadata, logs, local agent output, and operating
system files. The module-level ignore files remain because they include
module-specific historical rules; the archetype-resource `.gitignore`
also remains because it is part of generated-project behavior.

The active stale-reference audit found only
`io.github.coffeebuilder.it` in
`coffee-builder-maven-plugin/src/it/simple-it/pom.xml`. This remains
intentionally unchanged because it is the unrelated groupId of an
isolated integration-test project, as established by earlier phases.
Historical references in this migration document remain intentionally.

Validation:

-   root `mvn verify`: BUILD SUCCESS;
-   reactor order: parent, Maven plugin, minimal archetype;
-   Maven plugin: 148 tests, 0 failures, 0 errors, 0 skipped;
-   all four archetype integration scenarios passed;
-   both JARs contain the expected license and notice resources;
-   `git diff --check`: passed with no whitespace errors;
-   line-ending and Maven wrapper integrity checks: passed.

Pending EMO/IP confirmation:

-   the exact canonical transformation, if any, for the existing
    historical personal copyright lines;
-   the correct owner and creation year to use if headers are added to
    currently unheadered project files;
-   whether the retained Apache Software Foundation-origin Maven wrapper
    and archetype metadata content requires additional notice text in
    distributed artifacts.

Do not commit or push automatically.

------------------------------------------------------------------------

## Future phases

-   Phase 9 --- Maven publishing and Eclipse release preparation
-   Phase 10 --- Documentation and JakartaOne readiness
