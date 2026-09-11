---
description: "Workspace instructions for the plain-specification repository. Use when authoring, reviewing, or refactoring Java/Maven code, tests, and executor modules."
applyTo: "**/*.{java,xml,md,properties}"
---

# plain-specification workspace guidance

- Prefer small, isolated changes that preserve the existing Maven multi-module structure.
- Keep code consistent with Java 8 conventions and the repository's current dependency versions.
- For new features or bug fixes, update or add tests in the existing `src/test/java` packages.
- When modifying executor modules, preserve the structure of `plain-specification-executor/redis` and `plain-specification-executor/mybatis-plus` as separate implementation submodules.
- For documentation changes, keep Chinese and English text clear and minimal; do not remove the existing `开发文档.md` unless instructed.
- If behavior or design is unclear, ask for clarification before making large architectural changes.
