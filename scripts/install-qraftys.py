#!/usr/bin/env python3
"""Install a user-downloaded Qraftys pack locally, preserving glyphs and original ZIP.

Usage: python scripts/install-qraftys.py /path/to/qraftys.zip [game-directory]
Only pack.mcmeta compatibility metadata is adapted for the pinned Minecraft 26.2.
No third-party artwork is copied into src/main/resources or the distributable JAR.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import tempfile
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("source", type=Path)
parser.add_argument("game_directory", type=Path, nargs="?", default=Path("run"))
args = parser.parse_args()
source = args.source.resolve()
game = args.game_directory.resolve()
packs = game / "resourcepacks"
originals = game / "config/riserecode/fonts/originals"
packs.mkdir(parents=True, exist_ok=True)
originals.mkdir(parents=True, exist_ok=True)
digest = hashlib.sha256(source.read_bytes()).hexdigest()
backup = originals / ("qraftys-" + digest[:12] + ".zip")
if not backup.exists():
    shutil.copy2(source, backup)
target = packs / "qraftys-capitalized-font.zip"
if target.exists():
    old_digest = hashlib.sha256(target.read_bytes()).hexdigest()
    old_backup = originals / ("installed-qraftys-" + old_digest[:12] + ".zip")
    if not old_backup.exists():
        shutil.copy2(target, old_backup)
temporary = None
try:
    with zipfile.ZipFile(source) as incoming:
        incoming.getinfo("assets/minecraft/textures/font/ascii.png")
        metadata = json.loads(incoming.read("pack.mcmeta"))
        metadata["pack"].pop("pack_format", None)
        metadata["pack"].pop("supported_formats", None)
        metadata["pack"].update(min_format=[88, 0], max_format=[88, 0])
        with tempfile.NamedTemporaryFile(dir=packs, suffix=".tmp", delete=False) as handle:
            temporary = Path(handle.name)
        with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED) as outgoing:
            for entry in incoming.infolist():
                outgoing.writestr(entry, json.dumps(metadata, indent=2).encode() if entry.filename == "pack.mcmeta" else incoming.read(entry))
        with zipfile.ZipFile(temporary) as adapted:
            assert adapted.read("assets/minecraft/textures/font/ascii.png") == incoming.read("assets/minecraft/textures/font/ascii.png")
    temporary.replace(target)
finally:
    if temporary is not None:
        temporary.unlink(missing_ok=True)
print("Installed:", target)
print("Original:", backup)
print("Glyph artwork unchanged; compatibility metadata targets Minecraft 26.2 (88.0).")
