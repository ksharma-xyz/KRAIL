# Lint & Code Analysis

This file provides instructions for running linting and code analysis tools used in this project.

## Detekt

This project uses [Detekt](https://detekt.dev/) for static code analysis of Kotlin code.

### Run Detekt

The following commands can be used to run Detekt:

```bash
# All modules
./gradlew detekt

# Specific module
./gradlew :core:analytics:detekt
```

### Generate/Update Baseline

To generate or update the Detekt baseline, use the following commands:

```bash
# All modules
./gradlew detektBaseline

# Specific module
./gradlew :core:analytics:detektBaseline
```

### Available Tasks

To see all available Detekt tasks, you can run:

```bash
# List all detekt tasks
./gradlew tasks --group="verification" | grep detekt
```

### Baseline Files

- Global: `/config/baseline.xml`
- Module-specific: `[module]/baseline.xml`

### Common Commands

```bash
# Check code quality
./gradlew detekt

# Suppress existing issues (create baseline)
./gradlew detektBaseline
```
