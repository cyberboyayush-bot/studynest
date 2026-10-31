#!/usr/bin/env bash
# Records the emulator while scripts/demo.py drives the RELEASE build against freshly seeded demo
# data, then stitches the clips into artifacts/studynest-demo.mp4 plus a 2x cut.
#
# Usage: scripts/record.sh            (expects the API server running on :8787)
# Env:   ANDROID_SERIAL to pick a device, SKIP_BUILD=1 to reuse an existing release APK.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="$HOME/.bun/bin:$PATH"
adb="${ANDROID_HOME:-/opt/android-sdk}/platform-tools/adb"
out="$root/artifacts/studynest-demo.mp4"
fast="$root/artifacts/studynest-demo-2x.mp4"
apk="$root/android/app/build/outputs/apk/release/app-release.apk"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$root/artifacts"

if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  (cd "$root/android" && ./gradlew --console=plain -q assembleRelease)
fi

echo "Seeding demo data…"
(cd "$root/server" && bun run db:seed)

echo "Installing release build…"
"$adb" install -r "$apk" >/dev/null
cp "$apk" "$root/artifacts/StudyNest.apk"

# screenrecord stops after 3 minutes, so record back-to-back segments (170 s each) until told to stop.
"$adb" shell 'rm -f /sdcard/seg_*.mp4 /sdcard/stop_rec'
"$adb" shell 'i=0; while [ ! -f /sdcard/stop_rec ]; do screenrecord --bit-rate 8000000 --time-limit 170 /sdcard/seg_$i.mp4; i=$((i+1)); done' &
recorder=$!
sleep 2

status=0
python3 "$root/scripts/demo.py" --shots "$root/artifacts/screenshots" || status=$?

sleep 2
"$adb" shell 'touch /sdcard/stop_rec; pkill -INT screenrecord' || true
wait "$recorder" || true
sleep 1

for seg in $("$adb" shell 'ls /sdcard/seg_*.mp4' | tr -d '\r' | sort -V); do
  "$adb" pull -q "$seg" "$work/"
  echo "file '$work/$(basename "$seg")'" >> "$work/list.txt"
done
"$adb" shell 'rm -f /sdcard/seg_*.mp4 /sdcard/stop_rec'

ffmpeg -loglevel error -y -f concat -safe 0 -i "$work/list.txt" \
  -vf "scale=720:-2,fps=30" -c:v libx264 -preset slow -crf 23 -pix_fmt yuv420p -movflags +faststart "$out"
ffmpeg -loglevel error -y -i "$out" \
  -vf "setpts=0.5*PTS" -r 30 -c:v libx264 -preset slow -crf 23 -pix_fmt yuv420p -movflags +faststart -an "$fast"

echo "Video: $out"
echo "2x:    $fast"
exit $status
