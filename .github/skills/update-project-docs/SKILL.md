---
name: update-project-docs
description: Create, structure, audit, and update repository documentation from the current codebase, especially README.md, design or architecture documents, and coding-agent instruction files such as .github/copilot-instructions.md. Use after implementation, dependency, setup, build, architecture, operational, or contributor-workflow changes, or whenever these documents are missing, stale, inconsistent, or need a clearer structure.
---

# Update Project Docs

Create and maintain accurate repository documentation by deriving claims from source code, configuration, manifests, automation, and tests. Treat existing documentation as context to verify, not as proof of implementation.

Read [references/document-structure.md](references/document-structure.md) before creating or restructuring documentation.

## Workflow

### 1. Establish scope

- Read applicable repository instructions before acting.
- Run `git status --short` and preserve unrelated user changes.
- Identify the requested documentation files and their current locations.
- When the user names no paths, discover existing README, design or architecture, and instruction files before choosing conventional locations.
- Do not move or rename existing documents without a clear need or user authorization.
- Do not alter implementation merely to make a documentation claim true unless implementation work is also requested.

### 2. Discover the repository

Start with a file inventory using `rg --files`, excluding generated output, dependency directories, caches, and vendor directories. Determine the project type from evidence such as:

- Dependency and package manifests
- Lockfiles and workspace definitions
- Build, task-runner, compiler, framework, and deployment configuration
- Environment examples and configuration schemas
- CI/CD workflows and release automation
- Application entrypoints and package/module boundaries
- Tests, linting, formatting, typechecking, and validation configuration
- Database schemas, migrations, API contracts, and infrastructure definitions

Read the implementation relevant to each documentation claim. Use targeted `rg` searches to trace declared settings, commands, features, data models, and runtime behavior to their actual consumers.

### 3. Build an evidence map

Connect proposed claims to primary evidence before writing:

| Claim kind | Prefer evidence from |
| --- | --- |
| Prerequisites and installation | manifests, lockfiles, tool-version files, container configuration |
| Development, validation, build, and release commands | scripts, task definitions, CI workflows, build configuration |
| Technology stack | declared dependencies plus imports and runtime use |
| Features and user workflows | entrypoints, routes, handlers, components, tests |
| Architecture and boundaries | module layout, dependency direction, interfaces, configuration |
| Data storage and schemas | models, migrations, repositories, storage adapters |
| APIs and integrations | route definitions, clients, schemas, permissions, environment variables |
| Constraints and limitations | missing consumers, incomplete paths, explicit markers, unsupported targets |

Resolve contradictions in favor of current implementation. A declared dependency, type, flag, or configuration value does not prove the related behavior is active.

### 4. Create or update each document

- Use `README.md` as the operational entry point: explain what the project is, how to set it up, which verified commands to run, how to build or package it, and where deeper documentation lives.
- Use the repository’s design or architecture document as the implementation model: explain system boundaries, major flows, data ownership, technology choices, constraints, tradeoffs, and known limitations.
- Use the coding-agent instruction file as concise change guidance: capture invariants, conventions, architectural boundaries, sensitive coupled changes, prohibited assumptions, and verified validation commands.

When a file exists, preserve useful structure and tone unless restructuring materially improves discoverability. When it is missing, create it at the user-specified path or the repository’s established conventional path.

Avoid duplicating long explanations. Keep detailed architectural reasoning in the design document and link to it from the README and instruction file.

### 5. Verify cross-document consistency

Check that the documents agree on:

- Project name, scope, supported targets, and implemented features
- Required tools, supported versions, and setup steps
- Development, test, lint, typecheck, build, package, deploy, and run commands
- Output locations and generated artifacts
- Component boundaries, runtime flows, APIs, storage, and data ownership
- Configuration and environment requirements without exposing secrets
- Current limitations and intentionally unsupported behavior
- Repository-relative paths and project maps

Never invent a command. Copy it from a manifest, task definition, workflow, or authoritative tool configuration and explain any required working directory or prerequisite.

### 6. Validate

Always run:

```text
git diff --check
git diff -- <documentation paths>
```

Then run the cheapest relevant repository-defined checks. Discover them from manifests, task runners, CI, or contributor documentation rather than assuming a package manager or language. Run broader builds or tests when documentation changes depend on generated output, command behavior, or compiled configuration.

Check that:

- Referenced files and relative links exist.
- Code blocks use the appropriate shell or language label.
- Commands match the repository configuration exactly.
- Frontmatter remains valid for its host platform.
- No scaffold text, stale paths, unsupported claims, or secret values remain.

Report commands that passed, commands that failed, and checks that could not run. Separate environment failures from repository failures.

## Completion criteria

- The requested documents exist at the intended locations and serve distinct audiences.
- Setup and operational commands are complete, ordered, and traceable to repository configuration.
- Architecture statements and contributor rules are traceable to implementation.
- The documents are mutually consistent without unnecessary duplication.
- Unknown or unimplemented behavior is omitted or labeled clearly.
- Validation results and unresolved uncertainty are reported honestly.
