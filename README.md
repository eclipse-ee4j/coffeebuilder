# Eclipse Coffee Builder

Eclipse Coffee Builder for Jakarta EE helps developers create a small, understandable Jakarta EE project foundation and evolve it incrementally by adding capabilities as an application grows.

This monorepo contains:

- `coffee-builder-maven-plugin`: a Maven plugin that adds Jakarta EE capabilities to existing projects.
- `jakarta-ee-minimal-archetype`: a Maven archetype that generates minimal Jakarta EE projects.
- `configuration`: configuration catalogs packaged by the Maven plugin.

## Project resources

- [Eclipse project page](https://projects.eclipse.org/projects/ee4j.coffeebuilder)
- [Canonical source repository](https://github.com/eclipse-ee4j/coffeebuilder)
- [Issue tracker](https://github.com/eclipse-ee4j/coffeebuilder/issues)
- [Contributor guide](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

## Building

Eclipse Coffee Builder requires Java 21 and Maven. Build and verify the complete reactor from the repository root:

```bash
mvn verify
```

The reactor builds the parent project, the Coffee Builder Maven plugin, and the Jakarta EE minimal archetype.

## License

Eclipse Coffee Builder is distributed under the [Apache License, Version 2.0](LICENSE).
