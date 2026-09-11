# Contributing to plain-specification

Thank you for your interest in contributing to plain-specification! This document provides guidelines and information for contributors.

## Javadoc Author Requirement

All Java source files in this repository must include a Javadoc comment at the top-level type (class/interface/annotation) that contains an `@author` tag.

- Author to use: `Jayden.Liang`
- Purpose: Keep consistent attribution across files and ensure documentation tools capture the author.

How to check locally:

- There's a helper script at `scripts/check-javadoc-author.ps1` that scans the repo for Java files missing the author tag.

CI:

- Optionally add a step that runs the script and fails the build if any files are missing the author tag.

## Code of Conduct

This project adheres to a simple principle: be respectful and constructive. Treat all contributors with respect and focus on the technical merits of contributions.

## How to Contribute

### Reporting Bugs

Before creating an issue:
1. Check existing issues to avoid duplicates
2. Include a clear description, steps to reproduce, expected vs actual behavior
3. Provide environment details (Java version, Spring version, module affected)
4. Include code samples or test cases demonstrating the issue

### Suggesting Enhancements

Enhancement suggestions are welcome! Please:
1. Open an issue describing the enhancement and its use case
2. Consider implementation complexity and backward compatibility
3. Label the issue appropriately

### Pull Requests

1. **Fork the repository** and create a branch:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```

2. **Follow the coding standards** (see below)

3. **Write tests**:
   - Unit tests for new features
   - Test edge cases and error conditions
   - Run `mvn test` to verify no regressions

4. **Update documentation**:
   - Update README.md if adding features
   - Add Javadoc comments to public APIs
   - Include `@author Jayden.Liang` in all new Java files

5. **Commit messages**:
   - Use clear, descriptive commit messages
   - Reference issue numbers: `Fix #123: Add support for...`
   - Use imperative mood: "Add feature" not "Added feature"

6. **Submit the PR**:
   - Describe what changed and why
   - Link related issues
   - Include test results

## Development Setup

### Prerequisites

- JDK 8 or higher
- Maven 3.6+
- Git

### Building the Project

```bash
# Clone the repository
git clone https://github.com/yourusername/plain-specification.git
cd plain-specification

# Build all modules
mvn clean install

# Run tests
mvn test

# Build specific module
mvn clean install -pl plain-specification-core
```

## Coding Standards

### General

- Java 8 compatible syntax (source/target level 8)
- Use Lombok annotations where appropriate (`@Getter`, `@Slf4j`, etc.)
- Follow existing naming conventions
- Add Javadoc for public APIs with `@author Jayden.Liang`
- No trailing whitespace
- 4-space indentation

### Null Safety

- Always handle null values explicitly
- Use null checks in visitor methods
- Document null behavior in Javadoc
- Follow SQL NULL semantics where appropriate

### Type Safety

- Use generics consistently
- Avoid raw types — use `addExpressionType()` helper instead of unchecked casts
- Prefer `SFunction<T, R>` over raw lambdas for type safety

### Adding New Expression Types

When adding a new expression type:

1. Create the expression class in `core/expression/`
2. Add visitor method(s) to `IExpressionVisitor`
3. Implement in all visitor implementations:
   - `PredicateExpressionVisitor` (in-memory evaluation)
   - `MpQueryWrapperVisitor` (MyBatis-Plus SQL generation)
   - `AbstractExpressionVisitor` (base class defaults)
4. Add to `supportedExpressions()` in executor implementations
5. Add builder method to `Expressions<T>`
6. Write unit tests covering normal cases, null handling, and edge cases

### Testing

- Use JUnit 5 (`@Test` from `org.junit.jupiter.api`)
- Use descriptive test method names: `methodName_condition_expectedBehavior`
- Test both positive and negative cases
- Use `@BeforeEach` for setup
- When using `Expressions.create()` with method references, use explicit type parameter: `Expressions.<User>create()`

## Architecture Overview

### Core Concepts

- **Specification** — Entry point for building queries
- **Expressions** — Fluent DSL for building type-safe expression trees (18 types)
- **IExpressionVisitor** — Compiles expression trees to different targets (Predicate, QueryWrapper, Comparator)
- **ISpecificationEvaluator** — Executes queries (in-memory or hybrid pushdown)
- **ISpecificationExecutor** — Storage backend SPI

### Module Structure

```
plain-specification-core/          Core DSL, visitor pattern, hybrid execution
plain-specification-executor/
├── mybatis-plus/                  SQL generation via QueryWrapper
└── redis/                         ZSET + HASH storage with Pipeline
```

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
