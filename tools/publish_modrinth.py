"""Publish the built AIOA artifacts for one Minecraft release to Modrinth."""
import argparse
import json
import os
from pathlib import Path
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zipfile

from build_target import PROFILES, ROOT, available_loaders

API = "https://api.modrinth.com/v2"
PROJECT = "u1nPcVyt"
FABRIC_API = "P7dR8mSH"
CHANGELOG = ("Rebuilt the behavior editor, fixed tutorial and mob picker navigation, and added Lua scripting "
             "with recoverable drafts. Includes refined zombie pursuit and Horde coordination.")


def request(path, token, body=None, boundary=None):
    headers = {"Authorization": token, "User-Agent": "The-Intrusive-Thoughts/AllInOneApocalypse release"}
    if boundary:
        headers["Content-Type"] = "multipart/form-data; boundary=" + boundary
    with urllib.request.urlopen(urllib.request.Request(API + path, body, headers), timeout=90) as response:
        return json.load(response)


def versions(game, token):
    query = urllib.parse.urlencode({"game_versions": json.dumps([game]), "cache": time.time_ns()})
    return {entry["version_number"]: entry for entry in request(f"/project/{PROJECT}/version?{query}", token)}


def publish(game, loader, release, token):
    path = ROOT / f"build/releases/{release}/{game}/aioa-{loader}-{game}-{release}.jar"
    with zipfile.ZipFile(path) as package:
        if package.testzip() or "org/luaj/vm2/LuaValue.class" not in package.namelist():
            raise ValueError(f"Incomplete release artifact: {path}")
    existing = versions(game, token)
    number = f"{release}+mc{game}-{loader}"
    if number in existing and existing[number].get("changelog") != CHANGELOG:
        # The initial upload predates the picker and Lua changes.
        number += ".r2"
    loaders = ["fabric", "quilt"] if loader == "fabric" else [loader]
    if game == "1.20.1" and loader == "forge":
        loaders.append("neoforge")
    label = {"fabric": "Fabric / Quilt", "forge": "Forge", "neoforge": "NeoForge"}[loader]
    metadata = {
        "name": f"AIOA {release} · Minecraft {game} · {label}",
        "version_number": number,
        "changelog": CHANGELOG,
        "dependencies": [{"project_id": FABRIC_API, "dependency_type": "required"}] if loader == "fabric" else [],
        "game_versions": [game], "loaders": loaders, "version_type": "release",
        "featured": False, "project_id": PROJECT, "file_parts": ["jar"], "primary_file": "jar",
    }
    if number in existing:
        print(f"{number} already published as {existing[number]['id']}", flush=True)
        return
    boundary = "aioa-" + uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="data"\r\nContent-Type: application/json\r\n\r\n'.encode()
            + json.dumps(metadata).encode()
            + f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="jar"; filename="{path.name}"\r\nContent-Type: application/java-archive\r\n\r\n'.encode()
            + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode())
    for attempt in range(3):
        try:
            result = request("/version", token, body, boundary)
            print(f"{number} published as {result['id']}", flush=True)
            return
        except urllib.error.HTTPError as error:
            if error.code != 429 and error.code < 500:
                raise RuntimeError(f"{number}: {error.code} {error.read().decode(errors='replace')}") from None
        except (TimeoutError, urllib.error.URLError):
            pass
        time.sleep(3 * (attempt + 1))
        if number in versions(game, token):
            print(f"{number} confirmed after retry", flush=True)
            return
    raise RuntimeError(f"Could not complete the upload for {number}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("game", choices=PROFILES)
    parser.add_argument("loader", choices=["fabric", "forge", "neoforge"])
    parser.add_argument("release")
    args = parser.parse_args()
    if args.loader not in available_loaders(args.game):
        parser.error(f"{args.loader} is unavailable for {args.game}")
    token = os.environ.get("MODRINTH_TOKEN")
    if not token:
        parser.error("MODRINTH_TOKEN is required")
    publish(args.game, args.loader, args.release, token)


if __name__ == "__main__":
    main()
