
set -euo pipefail

RECORDING_DIR="${RECORDING_DIR:-e2e-video-recordings}"
REPORT_DIR="${REPORT_DIR:-e2e-report}"

FLOWS=(
  "us_1_1"
  "us_1_2"
  "us_1_3"
  "us_1_5"
  "us_1_6"
  "us_2_1"
  "us_2_3"
  "us_2_5"
  "us_2_6"
)

mkdir -p "$RECORDING_DIR"
mkdir -p "$REPORT_DIR"

echo ""
echo "==============================="
echo "           E2E Tests           "
echo "  $(date '+%Y-%m-%d %H:%M:%S') "
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
  VIDEO_FILE="${RECORDING_DIR}/${FLOW}.mp4"

  echo "-----------------------------"
  echo " Running: $FLOW"
  echo " Video: $VIDEO_FILE"
  echo "-----------------------------"

# TODO: Change to maestro cloud for recordings, --record does not supported on maestro 2.x
  if maestro test \
      --record "$VIDEO_FILE" \
      --format junit \
      --output "${REPORT_DIR}/${FLOW}.xml" \
      "$FLOW_FILE"; then
    echo "  PASSED: $FLOW"
    PASSED=$((PASSED + 1))
  else
    echo "  FAILED: $FLOW (see $VIDEO_FILE)"
    FAILED=$((FAILED + 1))
    FAILED_FLOWS+=("$FLOW")
  fi
  echo ""
done

echo "Merging reports to ${REPORT_DIR}/report.xml ..."

cat > "${REPORT_DIR}/report.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="E2E Tests" tests="${#FLOWS[@]}" failures="$FAILED" timestamp="$(date -u =%Y-%m-%dT%H:%M:%SZ)">
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
    echo "  See ${RECORDING_DIR}/${flow}.mp4"
  done
  echo ""
fi

echo " Recording: $RECORDING_DIR/"
echo " Report: $REPORT_DIR/report.xml"
echo "==============================="
echo ""

if [ "$FAILED" -gt 0 ]; then
  exit 1
fi