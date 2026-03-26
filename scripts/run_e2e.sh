
set -euo pipefail

RECORDING_DIR="${RECORDING_DIR:-e2e-video-recordings}"
REPORT_DIR="${REPORT_DIR:-e2e-report}"

FLOWS=(
  "us_1_1"
)
mkdir -p "$RECORDING_DIR"
mkdir -p "$REPORT_DIR"

TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

echo ""
echo "==============================="
echo "           E2E Tests           "
echo "  $TIMESTAMP "
echo "==============================="
echo " Flows: ${#FLOWS[@]}"
echo " Recordings: $RECORDING_DIR/"
echo " Report: $REPORT_DIR/report.xml"
echo "==============================="
echo ""

PASSED=0
FAILED=0
FAILED_FLOWS=()

# Disable animations
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

for FLOW in "${FLOWS[@]}"; do
  FLOW_FILE=".maestro/flows/${FLOW}.yaml"
  VIDEO_DEVICE="/sdcard/${FLOW}.mp4"
  VIDEO_LOCAL="${RECORDING_DIR}/${FLOW}.mp4"

  echo "-----------------------------"
  echo " Running: $FLOW"
  echo " Video: $VIDEO_LOCAL"
  echo "-----------------------------"

  adb shell "screenrecord --verbose $VIDEO_DEVICE" &
  RECORD_PID=$!

  sleep 2


  if maestro test \
      --format junit \
      --output "${REPORT_DIR}/${FLOW}.xml" \
      "$FLOW_FILE"; then
    FLOW_RESULT="PASSED"
  else
    FLOW_RESULT="FAILED"
  fi
  echo ""

  kill $RECORD_PID 2>/dev/null || true
  sleep 1

  adb pull "$VIDEO_DEVICE" "$VIDEO_LOCAL" 2>/dev/null || true

  adb shell rm "$VIDEO_DEVICE" 2>/dev/null || true

  if [ "$FLOW_RESULT" = "PASSED" ]; then
    echo "  PASSED: $FLOW"
    PASSED=$((PASSED + 1))
  else
    echo "  FAILED: $FLOW (See maestro cloud video)"
    FAILED=$((FAILED + 1))
    FAILED_FLOWS+=("$FLOW")
  fi

  echo ""
done

echo "Merging reports to ${REPORT_DIR}/report.xml ..."

cat > "${REPORT_DIR}/report.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="E2E Tests" tests="${#FLOWS[@]}" failures="$FAILED" timestamp="$TIMESTAMP">
EOF

for FLOW in "${FLOWS[@]}"; do
  XML="${REPORT_DIR}/${FLOW}.xml"
  if [ -f "$XML" ]; then
    tail -n +2 "$XML" >> "${REPORT_DIR}/report.xml"
  fi
done

echo "</testsuites>" >>"${REPORT_DIR}/report.xml"

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
  echo ""
  echo " Failed flows:"
  for flow in "${FAILED_FLOWS[@]}"; do
    echo "  $flow"
    echo "  Watch: ${RECORDING_DIR}/${flow}.mp4"
  done
  echo ""
fi

echo " Recordings: $RECORDING_DIR/"
echo " Report: $REPORT_DIR/report.xml"
echo "==============================="
echo ""

if [ "$FAILED" -gt 0 ]; then
  exit 1
fi