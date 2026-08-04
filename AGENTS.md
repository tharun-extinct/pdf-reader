# NoxReader Agent Routing

Use progressive disclosure for PDF reader feature and architecture work:

1. Read [`feature-blueprints/README.md`](feature-blueprints/README.md) and classify the task by intent.
2. Load only the primary blueprint identified by the routing table.
3. Follow that blueprint's links to the applicable sections of [`.github/architecture.md`](.github/architecture.md).
4. Inspect the listed implementation and tests before changing behavior or status documentation.
5. Load an impact-check blueprint only when the change touches the shared behavior named in the manifest.

If no manifest row matches (for example, isolated CI, dependency, or general
documentation work), do not force a blueprint match. Inspect the directly
relevant files and load architecture context only if a shared contract changes.

Do not load every blueprint for an isolated feature task. Read all of
`.github/architecture.md` and every affected blueprint only for structural,
cross-cutting, persistence-wide, coordinate-contract, or ambiguous changes.

Current code and verified tests override stale status prose. Never describe
planned behavior as implemented. Do not run Gradle locally; GitHub Actions is
the build and test authority for this repository.


# Don'ts

- Don't read prompts.md