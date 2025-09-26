# CI/CD Architecture & Design Philosophy

This document explains the design decisions, architecture, and best practices behind our CI/CD
pipeline for the Krail Compose Multiplatform project.

## Architecture Overview

Our CI/CD pipeline follows a **clean separation of concerns** principle with **independent job
execution** and **parallel processing** for optimal performance.

```
┌─────────────────┐
│   code-quality  │ ← Runs Detekt checks
└─────────┬───────┘
          │ (blocks until passes)
          ▼
  ┌───────────────────────────────────┐
  │        Build Jobs (Parallel)      │
  ├─────────────┬─────────────┬───────┤
  │build-android│build-android│build  │
  │   -debug    │  -release   │ -ios  │
  └─────────────┴─────────────┴───────┘
          │           │
          ▼           ▼
  ┌─────────────┬─────────────┐
  │ distribute  │ distribute  │
  │   -debug    │  -release   │
  └─────────────┴─────────────┘
```

## Design Philosophy

### 1. **Separation of Concerns**

Each workflow file has a **single responsibility**:

- **`build.yml`** - Main orchestration (workflow dependencies)
- **`code-quality.yml`** - Static analysis and linting
- **`build-android.yml`** - Android build logic
- **`build-ios.yml`** - iOS build logic
- **`distribute-firebase.yml`** - Firebase Distribution logic
- **`distribute-google-play-manual.yml`** - Google Play manual release logic
- **`distribute-google-play.yml`** - Automated Google Play release logic

**Benefits:**

- Easy to maintain and debug
- Changes to build logic don't affect distribution logic
- Each file is focused and readable
- Multiple people can work on different parts independently

### 2. **Independent Checkouts by Design**

Each job performs its own `actions/checkout@v5` step. This is **intentional** and follows **industry
best practices**.

#### Why Not Share Checkouts?

❌ **Artifact Sharing Approach** (Complex):

```yaml
# Not recommended - adds complexity
- name: Download source code
  uses: actions/download-artifact@v4
  with:
    name: source-code-artifact
```

✅ **Independent Checkout Approach** (Industry Standard):

```yaml
# Recommended - simple and fast
- uses: actions/checkout@v5
```

#### Performance Reality

| Approach             | Time    | Complexity | Reliability |
|----------------------|---------|------------|-------------|
| Independent Checkout | ~15-30s | Low        | High        |
| Artifact Sharing     | ~20-45s | High       | Medium      |

**GitHub Actions checkout is highly optimized:**

- Uses shallow clones
- Has intelligent caching
- Parallel execution across different runners
- Network-optimized with CDN

### 3. **Quality Gate Pattern**

Code quality checks act as a **quality gate** that must pass before any builds start.

```yaml
needs: code-quality  # All builds depend on this
```

**Benefits:**

- **Fail Fast**: Catch issues before expensive builds
- **Resource Efficiency**: Don't waste CI minutes on bad code
- **Consistent Quality**: Enforces coding standards across all PRs
- **Developer Feedback**: Quick feedback on code quality issues

### 4. **Parallel Execution Strategy**

Once code quality passes, all build jobs run **in parallel**:

```yaml
build-android-debug: # ┐
  needs: code-quality  # ├─ All run in parallel
build-android-release: # │
  needs: code-quality  # │
build-ios: # │
  needs: code-quality  # ┘
```

**Benefits:**

- **Faster CI**: Builds don't wait for each other
- **Efficient Resource Usage**: Uses multiple runners simultaneously
- **Quick Feedback**: Developers get results faster
- **Platform Independence**: iOS and Android builds are isolated

## Industry Best Practices Adopted

### 1. **Reusable Workflows**

Following GitHub's recommended pattern:

```yaml
uses: ./.github/workflows/build-android.yml
with:
  variant: debug
secrets: inherit
```

**Benefits:**

- DRY (Don't Repeat Yourself) principle
- Centralized build logic
- Easy to maintain and update
- Consistent behavior across environments

### 2. **Environment-Specific Configurations**

```yaml
environment: Firebase  # Controls secret access
```

**Benefits:**

- Secure secret management
- Environment-specific deployments
- Compliance with security best practices

### 3. **Conditional Execution**

```yaml
if: github.event_name != 'pull_request' || github.event.pull_request.draft == false
```

**Benefits:**

- Skip CI on draft PRs (saves resources)
- Smart triggering based on git events
- Reduces unnecessary builds

## Workflow Dependencies & Data Flow

### 1. **Quality Gate Dependency**

```yaml
build-android-debug:
  needs: code-quality  # ← Blocks until Detekt passes
```

**Why This Design:**

- Prevents building broken code
- Saves CI resources
- Provides quick feedback to developers

### 2. **Build to Distribution Flow**

```yaml
distribute-debug-apk-firebase:
  needs: build-android-debug  # ← Waits for successful build
```

**Benefits:**

- Only distributes successful builds
- Clear dependency chain
- Easy to trace failures

## Common Questions & Answers

### Q: Why don't we share checkout between jobs?

**A:** This is intentional and follows industry standards:

- **GitHub, Microsoft, Google** all use independent checkouts
- Checkout is fast (~15-30 seconds) and highly cached
- Independent jobs are easier to debug and maintain
- Parallel execution is more efficient than sequential artifact sharing

### Q: Isn't multiple checkouts wasteful?

**A:** No, modern CI/CD is designed for this:

- GitHub Actions optimizes checkout with shallow clones
- Each job runs on a separate runner (parallel execution)
- Network costs are minimal with GitHub's CDN
- The complexity of artifact sharing outweighs the minimal time savings

## Best Practices Summary

✅ **DO:**

- Keep workflows focused on single responsibilities
- Use independent checkouts for each job
- Implement quality gates before expensive operations
- Leverage parallel execution for build jobs
- Use reusable workflows to avoid duplication

❌ **DON'T:**

- Share source code via artifacts (adds complexity)
- Mix build logic with distribution logic in same file
- Make jobs dependent on each other unnecessarily

---

This architecture prioritizes **maintainability**, **performance**, and **developer experience**
while following **industry-proven patterns** used by major organizations worldwide.
