---
description: "Workspace-level instructions for AI coding agents working on plain-specification."
---

# plain-specification agent instructions

- This is a Maven multi-module Java project.
- The root POM declares two main modules:
  - `plain-specification-core`: core specification interfaces, repository abstractions, expression/model definitions, paging and query contracts.
  - `plain-specification-executor`: concrete executor adapters for specific data sources.
- Current executor submodules:
  - `plain-specification-executor/mybatis-plus`: MyBatis-Plus adapter implementation.
  - `plain-specification-executor/redis`: Redis adapter implementation using Spring Data Redis.
- The primary design goal is to separate specification construction from data-source execution:
  - business code should use `ISpecification<T>` and repository abstractions
  - executor modules convert those specifications into concrete data access behavior
- Always preserve the module boundaries and do not collapse `core` and `executor` responsibilities.
- Target Java version is 8; maintain compatibility with existing Java 8 code and Maven configuration.
- Follow the repository conventions in `开发文档.md` and `CONTRIBUTING.md`:
  - add or update tests alongside code changes
  - maintain clear Javadoc and meaningful logging
  - use Spring configuration or constructor injection for configurable behavior
- Useful build/test commands:
  - `mvn test` at project root
  - `mvn -pl plain-specification-core test`
  - `mvn -pl plain-specification-executor/redis test`
  - `mvn -pl plain-specification-executor/mybatis-plus test`
- If adding a new executor, create a new module under `plain-specification-executor` and implement the core interfaces there.
- Prefer small, focused changes, and link to `开发文档.md` when explaining architecture or module purpose.
