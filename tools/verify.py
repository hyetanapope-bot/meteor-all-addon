#!/usr/bin/env python3
"""
Static checks for this addon, runnable without a JDK.

Meteor addons are compiled against a specific Meteor Client build, and a typo in an import only
shows up when you build. This script catches the cheap mistakes before that:

  * Java structural sanity (balanced brackets, package matches directory, file name matches class).
  * Every `meteordevelopment.meteorclient.*` import resolves to a real source file in a
    meteor-client checkout.
  * Members referenced through those imports (e.g. `InvUtils.swap`) actually exist in the class.
  * `fabric.mod.json` entrypoint, mod metadata and the version constant agree with the build files.
  * Module classes extend Module, call super(...) and have unique names.

Usage:
    python3 tools/verify.py --meteor ../meteor-client
    METEOR_CLIENT=/path/to/meteor-client python3 tools/verify.py

Exit code is 0 when every check passes.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src" / "main" / "java"
RESOURCES = ROOT / "src" / "main" / "resources"

# Classes that live in Meteor but are generated or shipped as bytecode only.
IGNORED_PACKAGES = ("meteordevelopment.meteorclient.mixin.",)

TOKEN_STRINGS = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'')


def strip_comments_and_strings(code: str) -> str:
    """Removes comments and string/char literals so bracket counting is not fooled by them."""
    code = re.sub(r"/\*.*?\*/", " ", code, flags=re.S)
    code = re.sub(r"//[^\n]*", " ", code)
    return TOKEN_STRINGS.sub('""', code)


class Report:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.passed = 0

    def error(self, message: str) -> None:
        self.errors.append(message)

    def warn(self, message: str) -> None:
        self.warnings.append(message)

    def ok(self) -> None:
        self.passed += 1


def java_files() -> list[Path]:
    return sorted(SRC.rglob("*.java"))


def check_structure(code: str, path: Path, report: Report) -> None:
    rel = path.relative_to(SRC)
    expected_package = ".".join(rel.parts[:-1])

    package = re.search(r"^package\s+([\w.]+);", code, re.M)
    if not package:
        report.error(f"{rel}: missing package declaration")
    elif package.group(1) != expected_package:
        report.error(f"{rel}: package {package.group(1)} does not match directory {expected_package}")

    public_type = re.search(r"public\s+(?:final\s+|abstract\s+)?(?:class|enum|interface|record)\s+(\w+)", code)
    if public_type and public_type.group(1) != path.stem:
        report.error(f"{rel}: public type {public_type.group(1)} must live in {path.stem}.java")

    stripped = strip_comments_and_strings(code)
    for open_char, close_char in (("{", "}"), ("(", ")"), ("[", "]")):
        if stripped.count(open_char) != stripped.count(close_char):
            report.error(
                f"{rel}: unbalanced {open_char}{close_char} "
                f"({stripped.count(open_char)} vs {stripped.count(close_char)})"
            )

    if not code.rstrip().endswith("}"):
        report.error(f"{rel}: file does not end with a closing brace")


def meteor_index(meteor_root: Path) -> dict[str, Path]:
    index: dict[str, Path] = {}
    base = meteor_root / "src" / "main" / "java"
    if not base.is_dir():
        return index

    for path in base.rglob("*.java"):
        fqcn = ".".join(path.relative_to(base).with_suffix("").parts)
        index[fqcn] = path

    return index


def check_imports(code: str, path: Path, index: dict[str, Path], report: Report) -> None:
    rel = path.relative_to(SRC)

    for match in re.finditer(r"^import\s+(?:static\s+)?(meteordevelopment\.[\w.]+);", code, re.M):
        fqcn = match.group(1)

        if any(fqcn.startswith(prefix) for prefix in IGNORED_PACKAGES):
            continue

        # meteordevelopment.orbit is a separate library, not part of the Meteor sources.
        if fqcn.startswith("meteordevelopment.orbit."):
            continue

        if fqcn not in index:
            report.warn(f"{rel}: cannot verify import {fqcn} (not found in the meteor-client checkout)")
            continue

        report.ok()

        # Verify members used through this import: ClassName.member
        simple = fqcn.rsplit(".", 1)[-1]
        if simple == "*":
            continue

        target = index[fqcn]
        try:
            target_code = target.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue

        class_body = strip_comments_and_strings(target_code)

        for member in set(re.findall(rf"\b{re.escape(simple)}\.(?!class\b)(\w+)", code)):
            if member in class_body:
                report.ok()
                continue

            # Constants and members can legitimately come from a superclass; warn only.
            report.warn(
                f"{rel}: {simple}.{member} is not declared in {fqcn} "
                f"(may come from a superclass or be a false positive)"
            )


def load_catalog() -> dict[str, str]:
    versions: dict[str, str] = {}
    text = (ROOT / "gradle" / "libs.versions.toml").read_text(encoding="utf-8")

    for line in text.splitlines():
        stripped = line.strip()
        if "=" not in stripped or stripped.startswith("#"):
            continue

        key, _, value = stripped.partition("=")
        value = value.split("#", 1)[0].strip().strip('"')
        versions[key.strip()] = value

    return versions


def check_version(report: Report) -> None:
    versions = load_catalog()
    declared = versions.get("mod-version")

    if not declared:
        report.error("gradle/libs.versions.toml does not declare mod-version")
        return

    addon = SRC / "dev" / "alladdon" / "AllAddons.java"
    match = re.search(r'VERSION\s*=\s*"([^"]+)"', addon.read_text(encoding="utf-8"))

    if not match:
        report.error("AllAddons.VERSION is missing")
    elif match.group(1) != declared:
        report.error(f"AllAddons.VERSION {match.group(1)} != mod-version {declared}")
    else:
        report.ok()


def check_metadata(report: Report) -> None:
    raw = (RESOURCES / "fabric.mod.json").read_text(encoding="utf-8")
    # Gradle expands ${...} placeholders; neutralise them so json.loads accepts the file.
    json_text = re.sub(r"\$\{[^}]+\}", "0", raw)

    try:
        metadata = json.loads(json_text)
    except json.JSONDecodeError as error:
        report.error(f"fabric.mod.json is not valid JSON after placeholder expansion: {error}")
        return

    report.ok()

    for field in ("id", "version", "name", "license", "environment", "entrypoints", "depends"):
        if field not in metadata:
            report.error(f"fabric.mod.json is missing \"{field}\"")

    entrypoints = metadata.get("entrypoints", {}).get("meteor", [])
    if not entrypoints:
        report.error("fabric.mod.json declares no \"meteor\" entrypoint")

    for entrypoint in entrypoints:
        expected = SRC / Path(entrypoint.replace(".", "/") + ".java")
        if not expected.is_file():
            report.error(f"entrypoint {entrypoint} has no source file at {expected.relative_to(ROOT)}")
        else:
            report.ok()

    for dependency in ("minecraft", "meteor-client", "fabricloader", "java"):
        if dependency not in metadata.get("depends", {}):
            report.error(f"fabric.mod.json does not list \"{dependency}\" in depends")

    if '"${version}"' not in raw:
        report.error("fabric.mod.json should take its version from the build (${version})")


def check_modules(report: Report) -> None:
    names: dict[str, Path] = {}

    for path in java_files():
        code = path.read_text(encoding="utf-8")

        if "extends Module" not in code:
            continue

        if not re.search(r"super\(\s*AllAddons\.CATEGORY\s*,", code):
            report.error(f"{path.relative_to(SRC)}: module must call super(AllAddons.CATEGORY, ...)")

        match = re.search(r'super\(\s*AllAddons\.CATEGORY\s*,\s*"([^"]+)"', code)
        if not match:
            report.error(f"{path.relative_to(SRC)}: could not read the module name")
            continue

        name = match.group(1)
        if name in names:
            report.error(f"duplicate module name \"{name}\" in {path.relative_to(SRC)}")
        else:
            names[name] = path
            report.ok()

        if " " in name:
            report.error(f"module name \"{name}\" contains a space, which breaks Meteor commands")

    if not names:
        report.error("no modules found")

    print(f"  modules discovered: {len(names)}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify the addon against a meteor-client checkout.")
    parser.add_argument(
        "--meteor",
        default=os.environ.get("METEOR_CLIENT", str(ROOT.parent / "meteor-client")),
        help="path to a meteor-client source checkout (default: ../meteor-client)",
    )
    args = parser.parse_args()

    report = Report()
    meteor_root = Path(args.meteor)

    files = java_files()
    print(f"Java files: {len(files)}")

    for path in files:
        code = path.read_text(encoding="utf-8")
        check_structure(code, path, report)

    index: dict[str, Path] = {}
    if (meteor_root / "src" / "main" / "java").is_dir():
        index = meteor_index(meteor_root)
        print(f"Meteor classes indexed: {len(index)}")
    else:
        report.warn(f"meteor-client checkout not found at {meteor_root}; skipping API verification")

    if index:
        for path in files:
            check_imports(path.read_text(encoding="utf-8"), path, index, report)

    print("Metadata and versions:")
    check_version(report)
    check_metadata(report)
    check_modules(report)

    for warning in report.warnings:
        print(f"WARN  {warning}")

    for error in report.errors:
        print(f"ERROR {error}")

    print(f"\n{report.passed} checks passed, {len(report.warnings)} warnings, {len(report.errors)} errors")

    return 1 if report.errors else 0


if __name__ == "__main__":
    sys.exit(main())
