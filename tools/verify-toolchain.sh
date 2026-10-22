#!/usr/bin/env bash
# Verifica el toolchain Android de Kaby. Sale != 0 si algo falta.
set -euo pipefail
cd "$(dirname "$0")/.."
source tools/android-env.sh
fail=0
ok()  { printf "PASS  %s\n" "$1"; }
bad() { printf "FAIL  %s\n" "$1"; fail=1; }
has() { grep -q -- "$2" <<<"$1"; }

J=$(java -version 2>&1 | head -1)
has "$J" "17." && ok "JDK 17 ($J)" || bad "JDK 17 ($J)"
G=$(gradle --version 2>/dev/null | grep -E '^Gradle ' || true)
has "$G" "9.8.2" && ok "Gradle 9.8.2" || bad "Gradle 9.8.2 ($G)"
[ -f "$ANDROID_HOME/platforms/android-37.0/android.jar" ] && ok "platform android-37.0" || bad "platform android-37.0"
[ -d "$ANDROID_HOME/build-tools/37.0.0" ] && ok "build-tools 37.0.0" || bad "build-tools 37.0.0"
[ -x "$ANDROID_HOME/platform-tools/adb" ] && ok "platform-tools/adb" || bad "platform-tools/adb"
[ -f local.properties ] && grep -q '^sdk.dir=' local.properties && ok "local.properties" || bad "local.properties"
if [ -x ./gradlew ]; then
  ./gradlew --version >/dev/null 2>&1 && ok "wrapper ./gradlew" || bad "wrapper ./gradlew"
else
  bad "wrapper ./gradlew (falta)"
fi
echo "---"
if [ "$fail" -eq 0 ]; then echo "TOOLCHAIN OK"; else echo "TOOLCHAIN INCOMPLETO"; fi
exit "$fail"
