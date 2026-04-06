#!/usr/bin/env bash
set -euo pipefail

export PATH="$PATH:$HOME/.maestro/bin"

if ! command -v maestro >/dev/null 2>&1; then
  echo "Maestro CLI not found in PATH"
  exit 1
fi

REPORT_DIR="${REPORT_DIR:-e2e-report}"
TEST_OUTPUT_DIR="${TEST_OUTPUT_DIR:-e2e-test-output}"

FLOWS=(
  "us_4_1"
  "us_4_2"
  "us_4_6"
  "us_5_1"
  "us_5_2"

)

mkdir -p "$REPORT_DIR"
mkdir -p "$TEST_OUTPUT_DIR"

echo ""
echo "==============================="
echo "           E2E Tests           "
echo "  $(date '+%Y-%m-%d %H:%M:%S') "
echo "==============================="
echo " Flows: ${#FLOWS[@]}"
echo " Report: $REPORT_DIR/report.xml"
echo " Artifacts: $TEST_OUTPUT_DIR/"
echo "==============================="
echo ""

PASSED=0
FAILED=0
FAILED_FLOWS=()

APP_ID="com.example.myapplication"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

ADB="${ADB:-adb}"

echo "==== ADB check ===="
command -v "$ADB" >/dev/null 2>&1 || {
  echo "adb not found: $ADB"
  exit 1
}
"$ADB" version || true

echo "==== Wait for emulator ===="
"$ADB" wait-for-device
sleep 5

echo "==== Device list ===="
"$ADB" devices || true

echo "==== Disable animations ===="
"$ADB" shell settings put global window_animation_scale 0 || true
"$ADB" shell settings put global transition_animation_scale 0 || true
"$ADB" shell settings put global animator_duration_scale 0 || true

echo "Installing APK..."
"$ADB" install -r "$APK_PATH"

echo "==== Installed package check ===="
"$ADB" shell pm list packages | grep myapplication || true

echo "==== Launchable activity check ===="
"$ADB" shell cmd package resolve-activity --brief "$APP_ID" || true

echo "==== Manual launch test ===="
"$ADB" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 || true

echo "==== Give app time to settle ===="
sleep 10

echo "==== Focused app check ===="
"$ADB" shell dumpsys window windows | grep -E "mCurrentFocus|mFocusedApp" || true

echo "==== Recent crash logs ===="
"$ADB" logcat -d | tail -n 300 || true

for FLOW in "${FLOWS[@]}"; do
  FLOW_FILE=".maestro/flows/${FLOW}.yaml"

  echo "-----------------------------"
  echo " Running: $FLOW"
  echo "-----------------------------"

  if maestro test \
      --format junit \
      --output "${REPORT_DIR}/${FLOW}.xml" \
      --test-output-dir "${TEST_OUTPUT_DIR}/${FLOW}" \
      "$FLOW_FILE"; then
    echo "  PASSED: $FLOW"
    PASSED=$((PASSED + 1))
  else
    echo "  FAILED: $FLOW"
    FAILED=$((FAILED + 1))
    FAILED_FLOWS+=("$FLOW")
  fi
  echo ""
done

echo "Merging reports to ${REPORT_DIR}/report.xml ..."

cat > "${REPORT_DIR}/report.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="E2E Tests" tests="${#FLOWS[@]}" failures="$FAILED" timestamp="$(date -u '+%Y-%m-%dT%H:%M:%SZ')">
EOF

for FLOW in "${FLOWS[@]}"; do
  XML="${REPORT_DIR}/${FLOW}.xml"
  if [ -f "$XML" ]; then
    tail -n +2 "$XML" >> "${REPORT_DIR}/report.xml"
  fi
done

echo "</testsuites>" >> "${REPORT_DIR}/report.xml"

TOTAL=${#FLOWS[@]}

echo ""
echo "==============================="
echo " E2E RESULTS"
echo "..............................."
echo " Passed: $PASSED / $TOTAL"
echo " Failed: $FAILED / $TOTAL"
echo "==============================="
echo ""

if [ ${#FAILED_FLOWS[@]} -gt 0 ]; then
  echo " Failed flows:"
  for flow in "${FAILED_FLOWS[@]}"; do
    echo "  $flow"
  done
  echo ""
fi

echo " Report: $REPORT_DIR/report.xml"
echo " Artifacts: $TEST_OUTPUT_DIR/"
echo "==============================="
echo ""

if [ "$FAILED" -gt 0 ]; then
  exit 1
fi
