#!/usr/bin/env bash
# Builds RadioCraft and installs it plus its runtime deps into a Prism Launcher
# instance ("RadioCraft-Dev": Minecraft 1.21.1 + NeoForge 21.1.194).
# Usage: tools/deploy-prism.sh [--no-build]
set -euo pipefail
cd "$(dirname "$0")/.."

PRISM="${PRISM_DATA:-$APPDATA/PrismLauncher}"
INST="$PRISM/instances/RadioCraft-Dev"
MODS="$INST/minecraft/mods"
NEO=$(grep -m1 -E '^neoforge = "' gradle/libs.versions.toml | cut -d'"' -f2)
MC=$(grep -m1 -E '^minecraft = "' gradle/libs.versions.toml | cut -d'"' -f2)

[[ "${1:-}" == "--no-build" ]] || ./gradlew jar --console=plain -q

mkdir -p "$MODS"
if [[ ! -f "$INST/instance.cfg" ]]; then
  cat > "$INST/instance.cfg" <<CFG
[General]
ConfigVersion=1.2
InstanceType=OneSix
name=RadioCraft Dev
iconKey=default
CFG
fi
cat > "$INST/mmc-pack.json" <<PACK
{
    "components": [
        { "uid": "net.minecraft", "version": "$MC", "important": true },
        { "uid": "net.neoforged", "version": "$NEO" }
    ],
    "formatVersion": 1
}
PACK

rm -f "$MODS"/radiocraft-*.jar
cp build/libs/radiocraft-neoforge-*.jar "$MODS"/
# keep only the main jar
rm -f "$MODS"/*-sources.jar "$MODS"/*-javadoc.jar

C=~/.gradle/caches/modules-2/files-2.1
for pat in "curse.maven/simple-voice-chat-416089" "software.bernie.geckolib/geckolib-neoforge-$MC" \
           "maven.modrinth/jade" "mcjty.theoneprobe/theoneprobe"; do
  find "$C/$pat" -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -exec cp -u {} "$MODS"/ \;
done
echo "Installed into $MODS:"; ls -1 "$MODS"
