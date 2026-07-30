# Repository documentation structure

Use these adaptable structures when creating or reorganizing project documentation. Preserve a repository’s established conventions when they remain clear and accurate; do not force every optional section into every project.

## README

Audience: a new user or contributor trying to understand, install, run, validate, and build the project.

Recommended order:

1. Project title
2. One-paragraph purpose and scope
3. Status, support, or important caveats when relevant
4. Requirements or prerequisites
5. Installation and initial setup
6. Configuration and environment variables
7. Development or local run commands
8. Validation commands: tests, lint, formatting, typecheck, or equivalent
9. Production build, packaging, deployment, or distribution
10. Usage and major implemented features
11. Architecture summary or project map
12. Troubleshooting
13. Links to design, contribution, API, operations, security, or license documents

Structure rules:

- Put the shortest successful setup path early.
- Give commands in execution order and state the working directory when it is not the repository root.
- Explain outputs that users must load, execute, publish, or inspect.
- Document only environment variables discoverable from safe configuration or example files; never include real secrets.
- Distinguish development, validation, production build, and release commands.
- Omit irrelevant sections rather than filling them with generic prose.

## Design or architecture document

Audience: maintainers evaluating how the implemented system works and how structural changes propagate.

Recommended sections:

1. Title
2. Purpose and document scope
3. Goals and non-goals when they clarify boundaries
4. Technology stack with evidence-based rationale
5. System context and runtime architecture
6. Entrypoints, components, modules, and ownership
7. Important request, event, job, or data flows
8. Data models, persistence, caching, and retention
9. External APIs, services, and integration boundaries
10. Security, privacy, permissions, and failure handling
11. Build, deployment, or runtime topology when relevant
12. Key decisions and tradeoffs
13. Project map
14. Current constraints, risks, and limitations

Structure rules:

- Describe the implemented system rather than a desired future system.
- Use a compact stack table with concern, implementation, and reason when it improves scanning.
- Use Mermaid only when relationships or sequences are materially clearer than prose.
- Include code or schema excerpts only for stable, central contracts.
- Make ownership and direction explicit: who calls whom, where state lives, and which layer may mutate it.
- Separate confirmed limitations from proposed enhancements.
- Date the document only when the repository has a convention for versioned architecture snapshots.

## Coding-agent instruction file

Audience: automated coding agents and contributors changing the repository.

Preserve any required platform-specific filename, location, frontmatter, or glob scope. When creating a new instruction file, first inspect nearby examples or platform requirements available in the repository.

Recommended sections:

1. Project and stack orientation
2. Product contract or core invariants
3. Architecture and module boundaries
4. Data, state, API, or storage ownership
5. Language and implementation conventions
6. Framework, runtime, security, or lifecycle constraints
7. Changes that require coordinated edits or migrations
8. Prohibited assumptions and known incomplete behavior
9. Required validation commands and manual checks

Structure rules:

- Write direct, actionable guidance instead of duplicating the design document.
- Explain why a constraint matters when that helps an agent apply it correctly.
- Name concrete repository paths only after confirming they exist.
- State coupled edits for schemas, contracts, generated artifacts, migrations, and compatibility changes.
- Do not present aspirational behavior as an invariant.
- Keep validation commands synchronized with manifests and CI.

## Repository discovery guide

Adapt discovery to the ecosystem found in the repository:

| Evidence | What it can establish |
| --- | --- |
| Package or dependency manifest | ecosystem, dependencies, scripts, package metadata |
| Lockfile | package manager and reproducible dependency state |
| Tool-version file | required runtime or SDK versions |
| Task runner or build configuration | supported commands, targets, outputs |
| CI/CD workflow | authoritative validation and release sequence |
| Container or infrastructure files | services, ports, deployment topology |
| Environment example or schema | required configuration names and safe defaults |
| Entrypoints and routing | exposed applications, commands, APIs, or jobs |
| Tests | intended behavior, edge cases, and supported interfaces |
| Database migrations or schemas | durable data contracts and upgrade rules |

Do not assume filenames or commands across ecosystems. Discover equivalents for JavaScript/TypeScript, Python, JVM, .NET, Go, Rust, mobile, infrastructure, monorepo, and mixed-language projects.

## Consistency rules

- README explains how to understand and operate the project.
- The design document explains how the implemented system works.
- The instruction file explains how to change the system safely.
- Prefer one detailed source of truth and concise links from the other documents.
- Verify versions from authoritative configuration and actual use.
- Verify commands from executable project configuration.
- Verify feature claims through runtime consumers, routes, handlers, or tests.
- Label uncertainty instead of filling gaps with plausible assumptions.
