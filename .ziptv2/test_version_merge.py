"""Test del merge driver delle versioni del fork (ziptv2#554).

Lancio: python3 .ziptv2/test_version_merge.py
"""

import os
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
DRIVER = os.path.join(HERE, "version_merge.py")

PLUGIN = "packages/video_player/video_player_android"
PUBSPEC = f"{PLUGIN}/pubspec.yaml"
CHANGELOG = f"{PLUGIN}/CHANGELOG.md"


def pubspec(version, extra=""):
    return (
        "name: video_player_android\n"
        "description: Android implementation of the video_player plugin.\n"
        f"version: {version}\n"
        "\n"
        "environment:\n"
        "  sdk: ^3.12.0\n"
        f"{extra}"
        "\n"
        "dependencies:\n"
        "  meta: ^1.10.0\n"
    )


def changelog(*sections):
    return "".join(f"## {head}\n\n* {body}\n\n" for head, body in sections)


UPSTREAM_OLD = [("2.12.2", "Fix upstream 2.12.2."), ("2.12.1", "Fix upstream 2.12.1.")]
UPSTREAM_NEW = [("2.12.3", "Fix upstream 2.12.3.")] + UPSTREAM_OLD


def run_driver(base, ours, theirs, path):
    """Esegue il driver come lo chiama git (%O %A %B %P); torna (exit, %A)."""
    with tempfile.TemporaryDirectory() as tmp:
        files = {}
        for name, text in (("base", base), ("ours", ours), ("theirs", theirs)):
            files[name] = os.path.join(tmp, name)
            with open(files[name], "w", encoding="utf-8") as fh:
                fh.write(text)
        proc = subprocess.run(
            [sys.executable, DRIVER, files["base"], files["ours"], files["theirs"], path],
            capture_output=True,
            text=True,
        )
        with open(files["ours"], encoding="utf-8") as fh:
            return proc.returncode, fh.read()


class PubspecVersion(unittest.TestCase):
    def test_primo_commit_di_versione_sul_nuovo_tag(self):
        code, out = run_driver(
            pubspec("2.12.2"), pubspec("2.12.3"), pubspec("2.12.2+ziptv2.1"), PUBSPEC
        )
        self.assertEqual(code, 0)
        self.assertEqual(out, pubspec("2.12.3+ziptv2.1"))

    def test_commit_di_versione_successivo(self):
        # HEAD ha già il primo commit di versione riscritto sul nuovo tag
        code, out = run_driver(
            pubspec("2.12.2+ziptv2.1"),
            pubspec("2.12.3+ziptv2.1"),
            pubspec("2.12.2+ziptv2.2"),
            PUBSPEC,
        )
        self.assertEqual(code, 0)
        self.assertEqual(out, pubspec("2.12.3+ziptv2.2"))

    def test_altre_modifiche_si_fondono_con_merge_a_tre_vie(self):
        dep = "  flutter: \">=3.44.0\"\n"
        code, out = run_driver(
            pubspec("2.12.2"), pubspec("2.12.3", dep), pubspec("2.12.2+ziptv2.1"), PUBSPEC
        )
        self.assertEqual(code, 0)
        self.assertEqual(out, pubspec("2.12.3+ziptv2.1", dep))

    def test_versione_non_del_fork_su_entrambi_i_lati_e_conflitto(self):
        code, out = run_driver(pubspec("2.12.2"), pubspec("2.12.3"), pubspec("2.13.0"), PUBSPEC)
        self.assertEqual(code, 1)

    def test_conflitto_fuori_dalla_versione_resta_conflitto(self):
        code, out = run_driver(
            pubspec("2.12.2"),
            pubspec("2.12.3", "  flutter: \">=3.44.0\"\n"),
            pubspec("2.12.2+ziptv2.1", "  flutter: \">=3.40.0\"\n"),
            PUBSPEC,
        )
        self.assertEqual(code, 1)
        self.assertIn("<<<<<<<", out)


class Changelog(unittest.TestCase):
    def test_sezione_del_fork_in_cima_col_nuovo_tag(self):
        ours_entry = ("2.12.2+ziptv2.1", "Fork ziptv2: tracce Media3.")
        code, out = run_driver(
            changelog(*UPSTREAM_OLD),
            changelog(*UPSTREAM_NEW),
            changelog(ours_entry, *UPSTREAM_OLD),
            CHANGELOG,
        )
        self.assertEqual(code, 0)
        self.assertEqual(
            out, changelog(("2.12.3+ziptv2.1", "Fork ziptv2: tracce Media3."), *UPSTREAM_NEW)
        )

    def test_seconda_sezione_sopra_la_prima_gia_riscritta(self):
        first_old = ("2.12.2+ziptv2.1", "Fork ziptv2: tracce Media3.")
        first_new = ("2.12.3+ziptv2.1", "Fork ziptv2: tracce Media3.")
        second = ("2.12.2+ziptv2.2", "Fork ziptv2: transfer colore.")
        code, out = run_driver(
            changelog(first_old, *UPSTREAM_OLD),
            changelog(first_new, *UPSTREAM_NEW),
            changelog(second, first_old, *UPSTREAM_OLD),
            CHANGELOG,
        )
        self.assertEqual(code, 0)
        self.assertEqual(
            out,
            changelog(("2.12.3+ziptv2.2", "Fork ziptv2: transfer colore."), first_new, *UPSTREAM_NEW),
        )

    def test_modifica_di_una_sezione_upstream_non_e_gestita(self):
        edited = [("2.12.2", "Fix upstream riscritto."), UPSTREAM_OLD[1]]
        code, out = run_driver(
            changelog(*UPSTREAM_OLD),
            changelog(*UPSTREAM_NEW),
            changelog(("2.12.2+ziptv2.1", "Fork."), *edited),
            CHANGELOG,
        )
        self.assertEqual(code, 1)

    def test_sezione_aggiunta_senza_suffisso_del_fork_non_e_gestita(self):
        code, out = run_driver(
            changelog(*UPSTREAM_OLD),
            changelog(*UPSTREAM_NEW),
            changelog(("2.12.9", "Non nostra."), *UPSTREAM_OLD),
            CHANGELOG,
        )
        self.assertEqual(code, 1)


class AltriFile(unittest.TestCase):
    def test_merge_a_tre_vie_pulito(self):
        code, out = run_driver("a\nb\nc\n", "a\nB\nc\n", "a\nb\nc\nd\n", "README.md")
        self.assertEqual(code, 0)
        self.assertEqual(out, "a\nB\nc\nd\n")

    def test_conflitto_lascia_i_marker(self):
        code, out = run_driver("a\n", "b\n", "c\n", "README.md")
        self.assertEqual(code, 1)
        self.assertIn("<<<<<<<", out)


if __name__ == "__main__":
    unittest.main()
