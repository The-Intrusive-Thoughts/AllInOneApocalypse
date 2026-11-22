"""Build a game target from shared sources and the ordered Minecraft API patches."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

from minecraft26 import port_client

ROOT = Path(__file__).resolve().parents[1]
PORTS = ROOT / "gradle/ports"
PROFILES = {row[0]: row[1:] for line in (PORTS / "versions.txt").read_text().splitlines()
            if line.strip() and not line.startswith("#") for row in [line.split()]}
LAYERS = list(dict.fromkeys(profile[0] for profile in PROFILES.values()))


def available_loaders(game):
    _, _, forge, neo, _, _ = PROFILES[game]
    return ["fabric"] + (["forge"] if forge != "-" else []) + (["neoforge"] if neo not in ("-", "shared") else [])


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")


def gradle_environment(game):
    """Use the matching build JDK when several Minecraft toolchains are installed."""
    major = 25 if game.startswith("26.") else 21
    candidates = [os.environ.get("JAVA_HOME"), os.environ.get(f"JAVA_HOME_{major}_X64")]
    javac = shutil.which("javac")
    if javac:
        candidates.append(Path(javac).resolve().parent.parent)
    roots = [Path.home() / ".gradle/jdks"]
    if os.name == "nt":
        programs = Path(os.environ.get("ProgramFiles", r"C:\Program Files"))
        roots.extend(programs / name for name in ("Eclipse Adoptium", "Java", "Microsoft"))
    elif sys.platform == "darwin":
        roots.append(Path("/Library/Java/JavaVirtualMachines"))
    else:
        roots.append(Path("/usr/lib/jvm"))
    for root in roots:
        if root.is_dir():
            candidates.extend(sorted(root.iterdir(), reverse=True))
    for candidate in filter(None, candidates):
        jdk = Path(candidate)
        if (jdk / "Contents/Home").is_dir():
            jdk = jdk / "Contents/Home"
        release = jdk / "release"
        if release.is_file() and (jdk / "bin" / ("javac.exe" if os.name == "nt" else "javac")).is_file():
            if any(line.startswith(f'JAVA_VERSION="{major}.') or line == f'JAVA_VERSION="{major}"'
                   for line in release.read_text(encoding="utf-8", errors="replace").splitlines()):
                return {**os.environ, "JAVA_HOME": str(jdk)}
    return os.environ.copy()


def prepare(game, selected):
    layer, fabric, forge, neo, midnight, menu = PROFILES[game]
    modern = game.startswith("26.")
    available = available_loaders(game)
    aliases = {"quilt": "fabric"}
    if neo == "shared":
        aliases["neoforge"] = "forge"
    loaders = available if selected == "all" else [aliases.get(selected, selected)]
    if any(loader not in available for loader in loaders):
        raise ValueError(f"{selected} has no configured build for {game}. Available: {', '.join(available)}")
    if game == "1.20.1":
        return ROOT, loaders

    target = ROOT / "build/targets" / game / selected
    target.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="aioa-src-") as work:
        staging = Path(work)
        for module in ("common", "fabric", "forge", "neoforge"):
            shutil.copytree(ROOT / module / "src", staging / module / "src")
        patches = [PORTS / f"{name}.patch" for name in LAYERS[1:LAYERS.index(layer) + 1]]
        patch_release = PORTS / game / "release.patch"
        if patch_release.is_file():
            patches.append(patch_release)
        for patch in patches:
            if patch.stat().st_size:
                subprocess.run(["git", "apply", "--ignore-space-change", "--whitespace=nowarn", str(patch)],
                               cwd=staging, check=True)

        if game in ("1.20.3", "1.20.4") and "neoforge" in loaders:
            package = staging / "neoforge/src/main/java/com/flubburr/aioa/neoforge"
            shutil.copy2(PORTS / game / "AioaLegacyNetwork.java", package)
            meta = staging / "neoforge/src/main/resources/META-INF/neoforge.mods.toml"
            write(meta.with_name("mods.toml"), meta.read_text().replace('type="required"', "mandatory=true"))
            meta.unlink()
        if modern:
            if game == "26.3":
                shutil.copy2(PORTS / game / "AioaNativeFileDialog.java", staging)
            port_client(staging, game)
        metadata = staging / "fabric/src/main/resources/fabric.mod.json"
        descriptor = json.loads(metadata.read_text(encoding="utf-8"))
        descriptor.pop("jars", None)  # Loom include writes the actual nested filenames.
        descriptor["depends"]["java"] = ">=${java_version}"
        if modern:
            descriptor["entrypoints"].pop("modmenu", None)
        write(metadata, json.dumps(descriptor, indent=2) + "\n")
        for module in ("common", *loaders):
            # Only remove obsolete generated inputs. Keep the Gradle cache and run directories.
            src = target / module / "src"
            new = staging / module / "src"
            for existing in src.rglob("*") if src.exists() else ():
                if existing.is_file() and not (new / existing.relative_to(src)).exists():
                    existing.unlink()
            shutil.copytree(new, src, dirs_exist_ok=True)

    java = 25 if modern else 17 if game in ("1.20.3", "1.20.4") else 21
    overrides = dict(minecraft_version=game, java_version=java, enabled_loaders=",".join(loaders),
                     fabric_version=fabric, forge_version=forge, neoforge_version=neo,
                     fabric_loader_version="0.20.1" if game == "26.3" else "0.19.3" if modern else "0.18.6",
                     midnightlib_version=midnight, modmenu_version=menu)
    if modern and "forge" in loaders:
        overrides["net.minecraftforge.gradle.merge-source-sets"] = "true"
    common = [line for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines()
              if line and not line.startswith("#") and line.split("=", 1)[0] not in overrides]
    write(target / "gradle.properties", "\n".join(common + [f"{k}={v}" for k,v in overrides.items()]) + "\n")
    shutil.copy2(PORTS / "settings.gradle", target / "settings.gradle")
    shutil.copy2(PORTS / "root.gradle", target / "build.gradle")
    for module in loaders:
        script = {"fabric": "unobfuscated", "forge": "forge26"}.get(module, module) if modern else module
        if module == "neoforge" and game in ("1.20.3", "1.20.5"):
            script = "neogradle"
        shutil.copy2(PORTS / f"{script}.gradle", target / module / "build.gradle")
    for name in ("gradlew", "gradlew.bat", "LICENSE"):
        shutil.copy2(ROOT / name, target / name)
    shutil.copytree(ROOT / "gradle/wrapper", target / "gradle/wrapper", dirs_exist_ok=True)
    if modern:
        wrapper = target / "gradle/wrapper/gradle-wrapper.properties"
        write(wrapper, wrapper.read_text().replace("gradle-8.14-bin", "gradle-9.5.1-bin"))
    return target, loaders


def main():
    parser = argparse.ArgumentParser(description=__doc__, epilog="Use a JDK 21 for 1.x and a JDK 25 for 26.x.")
    parser.add_argument("game", choices=PROFILES)
    parser.add_argument("--loader", choices=["all", "fabric", "quilt", "forge", "neoforge"], default="all")
    parser.add_argument("--prepare", action="store_true", help="Prepare an importable Gradle project without building")
    parser.add_argument("--task", default="build", help="Gradle task to run in each selected loader project")
    args = parser.parse_args()
    try:
        selections = [args.loader]
        if args.loader == "all" and not args.prepare and args.game != "1.20.1":
            selections = available_loaders(args.game)
        for selected in selections:
            target, loaders = prepare(args.game, selected)
            print(f"{args.game} ({', '.join(loaders)}) -> {target}", flush=True)
            if not args.prepare:
                wrapper = target / ("gradlew.bat" if os.name == "nt" else "gradlew")
                cmd = [str(wrapper)] if os.name == "nt" else ["sh", str(wrapper)]
                subprocess.run(cmd + [f":{loader}:{args.task}" for loader in loaders], cwd=target,
                               env=gradle_environment(args.game), check=True)
                if args.task in ("build", "assemble"):
                    props = dict(line.split("=", 1) for line in (target / "gradle.properties").read_text().splitlines()
                                 if line and not line.startswith("#") and "=" in line)
                    destination = ROOT / "build/releases" / props["version"] / args.game
                    destination.mkdir(parents=True, exist_ok=True)
                    for loader in loaders:
                        name = f"{props['mod_id']}-{loader}-{args.game}-{props['version']}.jar"
                        shutil.copy2(target / loader / "build/libs" / name, destination / name)
                    print(f"Jars: {destination}", flush=True)
        return 0
    except (ValueError, OSError, subprocess.CalledProcessError) as exc:
        print(exc, file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
