#!/usr/bin/env python3
"""Validate the progressively disclosed Markdown documentation contract."""

from __future__ import annotations

import re
import sys
from collections import Counter
from pathlib import Path
from urllib.parse import unquote, urlsplit


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
BLUEPRINT_DIRECTORY = REPOSITORY_ROOT / "feature-blueprints"
MANIFEST = BLUEPRINT_DIRECTORY / "README.md"
IGNORED_DIRECTORIES = {".git", ".gradle", ".idea", "build"}
REQUIRED_BLUEPRINT_SECTIONS = (
    "Outcome",
    "Current verified status",
    "Architecture dependencies",
    "Feature-specific implications",
    "Related blueprints",
    "Relevant implementation and tests",
    "Acceptance criteria",
    "Remaining gaps",
)

FENCE_PATTERN = re.compile(r"^\s*(```|~~~)")
HEADING_PATTERN = re.compile(r"^(#{1,6})\s+(.+?)\s*#*\s*$")
INLINE_LINK_PATTERN = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
REFERENCE_LINK_PATTERN = re.compile(r"^\s*\[[^\]]+\]:\s*(\S+)", re.MULTILINE)


def markdown_files() -> list[Path]:
    return sorted(
        path
        for path in REPOSITORY_ROOT.rglob("*.md")
        if not any(part in IGNORED_DIRECTORIES for part in path.relative_to(REPOSITORY_ROOT).parts)
    )


def visible_markdown(text: str) -> str:
    """Remove fenced code, whose link-like content is not documentation."""
    output: list[str] = []
    active_fence: str | None = None
    for line in text.splitlines():
        match = FENCE_PATTERN.match(line)
        if match:
            marker = match.group(1)
            if active_fence is None:
                active_fence = marker
            elif marker == active_fence:
                active_fence = None
            continue
        if active_fence is None:
            output.append(line)
    return "\n".join(output)


def github_anchor(text: str) -> str:
    """Approximate GitHub's heading fragment normalization."""
    text = re.sub(r"<[^>]+>", "", text)
    text = re.sub(r"[`*_~]", "", text).strip().lower()
    text = re.sub(r"[^\w\- ]", "", text, flags=re.UNICODE)
    return re.sub(r"\s+", "-", text)


def anchors_for(path: Path) -> set[str]:
    anchors: set[str] = set()
    occurrences: Counter[str] = Counter()
    for line in visible_markdown(path.read_text(encoding="utf-8")).splitlines():
        match = HEADING_PATTERN.match(line)
        if not match:
            continue
        base = github_anchor(match.group(2))
        suffix = occurrences[base]
        occurrences[base] += 1
        anchors.add(base if suffix == 0 else f"{base}-{suffix}")
    return anchors


def link_destinations(text: str) -> list[str]:
    text = visible_markdown(text)
    destinations = [match.group(1).strip() for match in INLINE_LINK_PATTERN.finditer(text)]
    destinations.extend(match.group(1).strip() for match in REFERENCE_LINK_PATTERN.finditer(text))
    return destinations


def normalize_destination(raw: str) -> str:
    if raw.startswith("<") and ">" in raw:
        return raw[1 : raw.index(">")]
    # Markdown permits an optional title after a non-angle-bracket destination.
    return raw.split(maxsplit=1)[0]


def check_local_links(files: list[Path]) -> list[str]:
    errors: list[str] = []
    anchor_cache: dict[Path, set[str]] = {}
    external_schemes = {"http", "https", "mailto", "tel", "data"}

    for source in files:
        text = source.read_text(encoding="utf-8")
        for raw_destination in link_destinations(text):
            destination = normalize_destination(raw_destination)
            parsed = urlsplit(destination)
            if parsed.scheme.lower() in external_schemes or parsed.netloc:
                continue

            relative_path = unquote(parsed.path)
            fragment = unquote(parsed.fragment).lower()
            if relative_path.startswith("/"):
                target = REPOSITORY_ROOT / relative_path.lstrip("/")
            elif relative_path:
                target = source.parent / relative_path
            else:
                target = source
            target = target.resolve()

            try:
                target.relative_to(REPOSITORY_ROOT.resolve())
            except ValueError:
                errors.append(f"{source.relative_to(REPOSITORY_ROOT)}: link escapes repository: {destination}")
                continue

            if not target.exists():
                errors.append(f"{source.relative_to(REPOSITORY_ROOT)}: missing link target: {destination}")
                continue

            if fragment:
                if target.suffix.lower() != ".md":
                    errors.append(
                        f"{source.relative_to(REPOSITORY_ROOT)}: fragment targets non-Markdown file: {destination}"
                    )
                    continue
                anchors = anchor_cache.setdefault(target, anchors_for(target))
                if fragment not in anchors:
                    errors.append(f"{source.relative_to(REPOSITORY_ROOT)}: missing heading anchor: {destination}")
    return errors


def h2_sections(path: Path) -> set[str]:
    sections: set[str] = set()
    for line in visible_markdown(path.read_text(encoding="utf-8")).splitlines():
        match = HEADING_PATTERN.match(line)
        if match and len(match.group(1)) == 2:
            sections.add(match.group(2).strip())
    return sections


def feature_blueprints() -> list[Path]:
    return sorted(path for path in BLUEPRINT_DIRECTORY.glob("*.md") if path.name != MANIFEST.name)


def check_blueprint_contract(blueprints: list[Path]) -> list[str]:
    errors: list[str] = []
    for blueprint in blueprints:
        sections = h2_sections(blueprint)
        for required in REQUIRED_BLUEPRINT_SECTIONS:
            if required not in sections:
                errors.append(f"{blueprint.relative_to(REPOSITORY_ROOT)}: missing required section: {required}")

        related_match = re.search(
            r"^## Related blueprints\s*$([\s\S]*?)(?=^##\s|\Z)",
            visible_markdown(blueprint.read_text(encoding="utf-8")),
            flags=re.MULTILINE,
        )
        related_text = related_match.group(1).lower() if related_match else ""
        for relationship in ("required", "impact checks"):
            if relationship not in related_text:
                errors.append(
                    f"{blueprint.relative_to(REPOSITORY_ROOT)}: Related blueprints must declare {relationship}"
                )
    return errors


def task_router_rows(manifest_text: str) -> list[list[str]]:
    match = re.search(r"^## Task router\s*$([\s\S]*?)(?=^##\s|\Z)", manifest_text, flags=re.MULTILINE)
    if not match:
        return []

    rows: list[list[str]] = []
    for line in match.group(1).splitlines():
        if not line.strip().startswith("|"):
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if len(cells) != 5 or cells[0] == "Task concepts and synonyms" or set(cells[0]) <= {"-", ":"}:
            continue
        rows.append(cells)
    return rows


def check_manifest_coverage(blueprints: list[Path]) -> list[str]:
    errors: list[str] = []
    rows = task_router_rows(MANIFEST.read_text(encoding="utf-8"))
    if not rows:
        return ["feature-blueprints/README.md: Task router has no data rows"]

    routed: Counter[str] = Counter()
    for row_number, cells in enumerate(rows, start=1):
        concepts, primary, architecture, code_and_tests, _impact_checks = cells
        if not concepts:
            errors.append(f"feature-blueprints/README.md: router row {row_number} has no task concepts")
        if not architecture:
            errors.append(f"feature-blueprints/README.md: router row {row_number} has no architecture sections")
        if not code_and_tests:
            errors.append(f"feature-blueprints/README.md: router row {row_number} has no code/test areas")

        primary_match = re.fullmatch(r"\[[^\]]+\]\(([^)#]+\.md)(?:#[^)]+)?\)", primary)
        if not primary_match:
            errors.append(
                f"feature-blueprints/README.md: router row {row_number} primary blueprint must be one Markdown link"
            )
            continue
        primary_path = (MANIFEST.parent / unquote(primary_match.group(1))).resolve()
        if primary_path.parent != BLUEPRINT_DIRECTORY.resolve():
            errors.append(
                f"feature-blueprints/README.md: router row {row_number} primary blueprint is outside feature-blueprints"
            )
            continue
        routed[primary_path.name] += 1

    expected = {path.name for path in blueprints}
    actual = set(routed)
    for missing in sorted(expected - actual):
        errors.append(f"feature-blueprints/README.md: blueprint is not routed: {missing}")
    for unknown in sorted(actual - expected):
        errors.append(f"feature-blueprints/README.md: routed file is not a feature blueprint: {unknown}")
    return errors


def main() -> int:
    files = markdown_files()
    blueprints = feature_blueprints()
    errors = [
        *check_local_links(files),
        *check_blueprint_contract(blueprints),
        *check_manifest_coverage(blueprints),
    ]

    if errors:
        print(f"Documentation validation failed with {len(errors)} error(s):", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        f"Documentation validation passed: {len(files)} Markdown files, "
        f"{len(blueprints)} feature blueprints, and all manifest routes checked."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
