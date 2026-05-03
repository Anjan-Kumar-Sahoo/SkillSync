# 12 — SonarCloud Configuration & Coverage Guide

> Version: 1.0 | Date: 2026-05-03 | Status: Active

---

## 1. Overview

SonarCloud is configured to analyze **both Backend (Java) and Frontend (TypeScript/React)** in a unified scan. This document explains the configuration, common pitfalls, and how to debug coverage discrepancies.

### Key Files

| File | Purpose |
|---|---|
| `sonar-project.properties` | SonarCloud scanner configuration (project root) |
| `.github/workflows/ci-cd.yml` | CI pipeline — builds, tests, verifies reports, runs Sonar |
| `Frontend/jest.config.cjs` | Jest test configuration — defines `collectCoverageFrom` |
| `Backend/<service>/pom.xml` | JaCoCo plugin configuration per microservice |

---

## 2. Architecture

```
┌──────────────────────────────────────────────────────────┐
│  CI Pipeline (GitHub Actions)                            │
│                                                          │
│  1. Build & Test Backend (mvn clean verify)              │
│     → Generates: Backend/<svc>/target/site/jacoco/*.xml  │
│                                                          │
│  2. Build & Test Frontend (npm run test:ci)              │
│     → Generates: Frontend/coverage/lcov.info             │
│                                                          │
│  3. Verify Reports Exist                                 │
│     → JaCoCo XMLs for all 10 modules                     │
│     → LCOV file with source file paths                   │
│                                                          │
│  4. SonarCloud Scan                                      │
│     → Reads sonar-project.properties                     │
│     → Ingests JaCoCo + LCOV                              │
│     → Reports combined coverage on dashboard             │
└──────────────────────────────────────────────────────────┘
```

---

## 3. Coverage Scope Alignment (CRITICAL)

### The Problem

Jest's `collectCoverageFrom` in `jest.config.cjs` only collects coverage from specific directories:

```js
collectCoverageFrom: [
  'src/App.tsx',
  'src/components/**/*.{ts,tsx}',
  'src/context/**/*.{ts,tsx}',
  'src/hooks/**/*.{ts,tsx}',
  'src/routes/**/*.{ts,tsx}',
  'src/services/**/*.{ts,tsx}',
  'src/store/**/*.{ts,tsx}',
  'src/utils/**/*.{ts,tsx}',
  '!src/main.tsx',
  '!src/types/**/*.d.ts',
  '!src/**/*.css',
]
```

**Files NOT in this list** (e.g., `pages/**`, `features/**`, `main.tsx`) will have **0% coverage** in the LCOV report. If SonarCloud analyzes these files but finds no coverage data, it reports them as 0% — dragging down the overall score.

### The Fix

`sonar.coverage.exclusions` in `sonar-project.properties` must exclude all files that Jest doesn't cover:

```properties
sonar.coverage.exclusions=\
  Frontend/src/main.tsx,\
  Frontend/src/vite-env.d.ts,\
  Frontend/src/types/**,\
  Frontend/src/pages/**,\
  Frontend/src/features/**,\
  Frontend/src/assets/**,\
  ...
```

> [!WARNING]
> If you add a new directory to Jest's `collectCoverageFrom`, you must also **remove** the corresponding entry from `sonar.coverage.exclusions`. Otherwise, Sonar will ignore coverage data that Jest provides.

---

## 4. LCOV Path Resolution

Jest generates LCOV with paths relative to the `Frontend/` directory:

```
SF:src/components/layout/Navbar.tsx
```

SonarCloud resolves these paths against the source path configured in `sonar.sources`:

```properties
sonar.sources=Backend,Frontend/src
```

Since `Frontend/src` is a source root, and the LCOV paths start with `src/`, Sonar resolves `src/components/layout/Navbar.tsx` relative to the project root as `Frontend/src/components/layout/Navbar.tsx`. This works correctly **as long as the Sonar scanner runs from the project root**.

> [!TIP]
> If coverage shows 0% despite a valid LCOV file, check the `SF:` paths in `lcov.info` and verify they resolve correctly from the project root.

---

## 5. Backend Coverage Configuration

Each backend microservice has its own JaCoCo plugin in `pom.xml`:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution><goals><goal>prepare-agent</goal></goals></execution>
    <execution>
      <id>report</id><phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

SonarCloud reads **per-module** reports (NOT the aggregate):

```properties
sonar.coverage.jacoco.xmlReportPaths=\
  Backend/auth-service/target/site/jacoco/jacoco.xml,\
  Backend/user-service/target/site/jacoco/jacoco.xml,\
  ...
```

> [!CAUTION]
> Do NOT use `Backend/target/site/jacoco-aggregate/jacoco.xml`. The aggregate report produces incomplete data because child modules use `spring-boot-starter-parent` (not the aggregator POM) as their Maven parent. Verified: aggregate shows AuthService at 25% vs per-module at 98%.

---

## 6. CI Execution Order

The strict execution order in `.github/workflows/ci-cd.yml` is:

```
1. mvn -B clean verify          → Backend JaCoCo reports generated
2. Verify JaCoCo reports exist  → Fail fast if missing
3. npm ci && npm run test:ci    → Frontend LCOV generated
4. Verify lcov.info exists      → Fail fast if missing
5. Debug log LCOV contents      → First 30 lines + SF paths
6. SonarCloud scan              → Ingests both report types
```

---

## 7. Debugging Coverage Issues

### Step 1: Check CI Logs

Look for these in the SonarCloud scan output:

| Log Message | Meaning |
|---|---|
| `Analysing LCOV file: Frontend/coverage/lcov.info` | ✅ Sonar found the LCOV file |
| `Coverage report loaded successfully` | ✅ Coverage data ingested |
| `Skipping file ... not in sources` | ⚠️ Path mismatch — file in LCOV doesn't match `sonar.sources` |
| `No coverage data for ...` | ⚠️ File is in sources but not in LCOV or coverage.exclusions |

### Step 2: Verify LCOV Content

```bash
# Check the first few source file paths
grep "^SF:" Frontend/coverage/lcov.info | head -10

# Count total source files
grep -c "^SF:" Frontend/coverage/lcov.info
```

### Step 3: Compare Scopes

```bash
# Files Jest covers
grep "^SF:" Frontend/coverage/lcov.info | sed 's/SF://' | sort

# Files Sonar would analyze (minus exclusions)
# Compare against sonar.inclusions - sonar.exclusions - sonar.coverage.exclusions
```

### Step 4: Enable Verbose Logging

Set in `sonar-project.properties` or as a CLI arg:

```
-Dsonar.verbose=true
```

This outputs detailed logs including which files were analyzed, which coverage data was applied, and which files were skipped.

---

## 8. Adding Coverage for New Files

When adding a new source directory (e.g., `Frontend/src/newModule/**`):

1. **Add to Jest coverage**: Update `collectCoverageFrom` in `Frontend/jest.config.cjs`
2. **Remove from Sonar exclusions**: Remove `Frontend/src/newModule/**` from `sonar.coverage.exclusions` in `sonar-project.properties`
3. **Write tests**: Ensure the new files have corresponding `.test.ts`/`.test.tsx` files
4. **Verify locally**: Run `npm run test:coverage` and check the coverage table

---

## 9. Quality Gate Configuration

The Quality Gate runs in **non-blocking mode**:

```properties
sonar.qualitygate.wait=false
```

Combined with `continue-on-error: true` on the `code-quality` job, this ensures:

- ✅ SonarCloud results are always reported
- ✅ CI pipeline never fails due to Sonar issues
- ✅ Deployments proceed regardless of Quality Gate status

To make the gate blocking (e.g., for PRs), change to:

```properties
sonar.qualitygate.wait=true
```
