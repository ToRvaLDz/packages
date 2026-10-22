#!/usr/bin/env python3
"""Merge driver git per le versioni del fork ziptv2 (ziptv2#554).

Ogni tag upstream cambia `version:` nel pubspec del plugin e apre una sezione in
cima al CHANGELOG; i nostri commit `chore: versione X+ziptv2.N` toccano le stesse
righe, quindi il rebase su un tag nuovo andrebbe sempre in conflitto. Il driver
risolve solo questo caso:

- pubspec.yaml: merge a tre vie con la riga `version:` messa da parte; se il
  commit applicato porta `+ziptv2.N`, la versione diventa `<base upstream>+ziptv2.N`.
- CHANGELOG.md: le sezioni `## X+ziptv2.N` aggiunte dal commit vanno in cima a
  quello che c'è già, rinominate sulla base upstream (`## <base>+ziptv2.N`).

Qualunque altra differenza passa per `git merge-file` (conflitto = marker ed exit 1).

Uso (da git, via `merge.ziptv2-version.driver`):
    version_merge.py %O %A %B %P
Scrive il risultato in %A; exit 0 se risolto, 1 se resta un conflitto.
"""

import os
import re
import subprocess
import sys
import tempfile

FORK_SUFFIX = "+ziptv2."
VERSION_RE = re.compile(r"^version:[ \t]*(\S+)[ \t]*$", re.MULTILINE)
VERSION_PLACEHOLDER = "version: __ziptv2_version__"
HEADING_RE = re.compile(r"^## ", re.MULTILINE)


class Unresolved(Exception):
    """Il driver non sa risolvere il caso: si ripiega sul merge a tre vie."""


def upstream_of(version):
    return version.split(FORK_SUFFIX, 1)[0]


def fork_build(version):
    """N di `X+ziptv2.N`, None per una versione non del fork."""
    if FORK_SUFFIX not in version:
        return None
    return version.split(FORK_SUFFIX, 1)[1]


def three_way(base, ours, theirs):
    """`git merge-file` sui testi; torna (testo, conflitto?)."""
    with tempfile.TemporaryDirectory() as tmp:
        paths = []
        for name, text in (("ours", ours), ("base", base), ("theirs", theirs)):
            path = os.path.join(tmp, name)
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(text)
            paths.append(path)
        proc = subprocess.run(
            ["git", "merge-file", "-p", "-L", "ours", "-L", "base", "-L", "theirs", *paths],
            capture_output=True,
            text=True,
        )
    if proc.returncode < 0 or proc.returncode > 127:
        raise RuntimeError(f"git merge-file fallito: {proc.stderr.strip()}")
    return proc.stdout, proc.returncode != 0


def merged_version(base, ours, theirs):
    if theirs == base:
        return ours
    if ours == base:
        return theirs
    build = fork_build(theirs)
    if build is None:
        raise Unresolved(f"versione {theirs} contro {ours}: non è un commit di versione del fork")
    return f"{upstream_of(ours)}{FORK_SUFFIX}{build}"


def merge_pubspec(base, ours, theirs):
    versions = []
    texts = []
    for text in (base, ours, theirs):
        found = VERSION_RE.findall(text)
        if len(found) != 1:
            raise Unresolved("pubspec senza una sola riga `version:`")
        versions.append(found[0])
        texts.append(VERSION_RE.sub(VERSION_PLACEHOLDER, text, count=1))
    version = merged_version(*versions)
    merged, conflict = three_way(*texts)
    if conflict:
        raise Unresolved("conflitto nel pubspec fuori dalla riga `version:`")
    return merged.replace(VERSION_PLACEHOLDER, f"version: {version}", 1)


def split_sections(text):
    """(preambolo, [sezioni `## ...` col loro corpo])."""
    starts = [m.start() for m in HEADING_RE.finditer(text)]
    if not starts:
        return text, []
    bounds = starts + [len(text)]
    return text[: starts[0]], [text[a:b] for a, b in zip(bounds, bounds[1:])]


def heading_of(section):
    return section.split("\n", 1)[0][len("## "):].strip()


def merge_changelog(base, ours, theirs):
    base_pre, base_sections = split_sections(base)
    theirs_pre, theirs_sections = split_sections(theirs)
    ours_pre, ours_sections = split_sections(ours)
    base_heads = {heading_of(s) for s in base_sections}
    added = [s for s in theirs_sections if heading_of(s) not in base_heads]
    kept = [s for s in theirs_sections if heading_of(s) in base_heads]
    if theirs_pre != base_pre or kept != base_sections:
        raise Unresolved("il commit cambia il CHANGELOG oltre ad aggiungere sezioni")
    if not ours_sections:
        raise Unresolved("CHANGELOG senza sezioni sul lato upstream")
    upstream = upstream_of(heading_of(ours_sections[0]))
    renamed = []
    for section in added:
        build = fork_build(heading_of(section))
        if build is None:
            raise Unresolved(f"sezione aggiunta non del fork: {heading_of(section)}")
        body = section.split("\n", 1)[1] if "\n" in section else ""
        renamed.append(f"## {upstream}{FORK_SUFFIX}{build}\n{body}")
    return ours_pre + "".join(renamed) + "".join(ours_sections)


def merge(base, ours, theirs, path):
    """Torna (testo, risolto?)."""
    name = os.path.basename(path)
    try:
        if name == "pubspec.yaml":
            return merge_pubspec(base, ours, theirs), True
        if name == "CHANGELOG.md":
            return merge_changelog(base, ours, theirs), True
    except Unresolved as exc:
        print(f"version_merge: {path}: {exc}", file=sys.stderr)
    merged, conflict = three_way(base, ours, theirs)
    return merged, not conflict


def read(path):
    with open(path, encoding="utf-8", newline="") as fh:
        return fh.read()


def main(argv):
    if len(argv) != 5:
        print("uso: version_merge.py %O %A %B %P", file=sys.stderr)
        return 2
    base_path, ours_path, theirs_path, path = argv[1:]
    text, resolved = merge(read(base_path), read(ours_path), read(theirs_path), path)
    with open(ours_path, "w", encoding="utf-8", newline="") as fh:
        fh.write(text)
    return 0 if resolved else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
