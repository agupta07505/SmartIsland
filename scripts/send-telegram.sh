#!/usr/bin/env bash
#
# Smart Island (2026)
# © Animesh Gupta — github.com/agupta07505
# Licensed under the GNU GPL v3 License
#
# Forwards build artifacts (APKs) to a Telegram group with topics.
#

set -euo pipefail

APK_PATH="${1:-}"
BUILD_TYPE="${2:-Release}"
CUSTOM_TOPIC="${3:-}"

if [[ -z "${TELEGRAM_BOT_TOKEN:-}" || -z "${TELEGRAM_CHAT_ID:-}" ]]; then
  echo "::notice ::Telegram secrets (TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID) are not configured. Skipping artifact forward."
  exit 0
fi

if [[ -z "$APK_PATH" || ! -f "$APK_PATH" ]]; then
  echo "::error ::Artifact file not found: '$APK_PATH'"
  exit 1
fi

escape_html() {
  local str="$1"
  str="${str//&/&amp;}"
  str="${str//</&lt;}"
  str="${str//>/&gt;}"
  echo "$str"
}

FILE_NAME="$(basename "$APK_PATH")"
FILE_SIZE="$(du -h "$APK_PATH" | cut -f1)"
REPO="${GITHUB_REPOSITORY:-agupta07505/SmartIsland}"
RUN_ID="${GITHUB_RUN_ID:-}"
RUN_URL="https://github.com/${REPO}/actions/runs/${RUN_ID}"
COMMIT_SHA="${GITHUB_SHA:-}"
SHORT_SHA="${COMMIT_SHA:0:7}"
BRANCH_OR_TAG="${GITHUB_REF_NAME:-}"
VERSION="${VERSION_TAG:-${BRANCH_OR_TAG}}"
ACTOR="$(escape_html "${GITHUB_ACTOR:-}")"
RUN_NUMBER="${GITHUB_RUN_NUMBER:-}"

# Gather recent commit messages (up to 3)
RECENT_COMMITS=""
while IFS= read -r line; do
  if [[ -n "$line" ]]; then
    sha="${line%% *}"
    msg="${line#* }"
    if [[ ${#msg} -gt 80 ]]; then
      msg="${msg:0:77}..."
    fi
    msg="$(escape_html "$msg")"
    RECENT_COMMITS+="  • <a href=\"https://github.com/${REPO}/commit/${sha}\">${sha:0:7}</a> ${msg}
"
  fi
done < <(git log -3 --pretty=format:'%H %s' 2>/dev/null || true)

# Determine target topic
TARGET_TOPIC="$CUSTOM_TOPIC"
if [[ -z "$TARGET_TOPIC" ]]; then
  if [[ "$BUILD_TYPE" =~ [Dd]ebug ]]; then
    TARGET_TOPIC="${TELEGRAM_DEBUG_TOPIC_ID:-${TELEGRAM_TOPIC_ID:-}}"
  else
    TARGET_TOPIC="${TELEGRAM_RELEASE_TOPIC_ID:-${TELEGRAM_TOPIC_ID:-}}"
  fi
fi

# ── Build HTML caption ──────────────────────────────────────────
if [[ "$BUILD_TYPE" =~ [Rr]elease ]]; then
  CAPTION="🚀 <b>Smart Island ${VERSION} — New Release</b>

━━━━━━━━━━━━━━━━━━━━━━

📦 <b>Artifact</b>
  <code>${FILE_NAME}</code>  (${FILE_SIZE})

🏷 <b>Version:</b>  <code>${VERSION}</code>
📱 <b>Android:</b>  8.0+ (API 26)
🔐 <b>Signed:</b>  ✅ Release keystore

━━━━━━━━━━━━━━━━━━━━━━

📝 <b>Recent Changes</b>
${RECENT_COMMITS}
━━━━━━━━━━━━━━━━━━━━━━

🔗 <b>Links</b>
  📥 <a href=\"https://github.com/${REPO}/releases/tag/${VERSION}\">Download from GitHub Releases</a>
  📋 <a href=\"https://github.com/${REPO}/blob/main/CHANGELOG.md\">Full Changelog</a>
  ⚙️ <a href=\"${RUN_URL}\">Workflow Run #${RUN_NUMBER}</a>
  🌐 <a href=\"https://smartisland.vercel.app\">Website</a>

━━━━━━━━━━━━━━━━━━━━━━
🏝️ <i>Smart Island — Dynamic Island for Android</i>
🔒 <i>No internet · No ads · No analytics · 100% on-device</i>"

else
  # Debug / Dev build
  CAPTION="🛠 <b>Smart Island Dev Build #${RUN_NUMBER}</b>

━━━━━━━━━━━━━━━━━━━━━━

📦 <b>Artifact</b>
  <code>${FILE_NAME}</code>  (${FILE_SIZE})

🌿 <b>Branch:</b>  <code>${BRANCH_OR_TAG}</code>
👤 <b>Author:</b>  ${ACTOR}
🔐 <b>Signed:</b>  Debug (not for distribution)

━━━━━━━━━━━━━━━━━━━━━━

📝 <b>Recent Commits</b>
${RECENT_COMMITS}
━━━━━━━━━━━━━━━━━━━━━━

🔗 <b>Links</b>
  🔀 <a href=\"https://github.com/${REPO}/tree/${BRANCH_OR_TAG}\">Branch: ${BRANCH_OR_TAG}</a>
  ⚙️ <a href=\"${RUN_URL}\">Workflow Run #${RUN_NUMBER}</a>
  📊 <a href=\"https://github.com/${REPO}/actions\">All CI Runs</a>

━━━━━━━━━━━━━━━━━━━━━━
🏝️ <i>Smart Island — Dev Testing Build</i>
⚠️ <i>For testing only — not signed for distribution</i>"
fi

# ── Normalize and send ──────────────────────────────────────────
TELEGRAM_BOT_TOKEN="$(echo "${TELEGRAM_BOT_TOKEN}" | tr -d '[:space:]')"
CHAT_ID="$(echo "${TELEGRAM_CHAT_ID}" | tr -d '[:space:]')"
TARGET_TOPIC="$(echo "${TARGET_TOPIC:-}" | tr -d '[:space:]')"

# Auto-prefix -100 for supergroup numeric IDs
if [[ "$CHAT_ID" =~ ^[0-9]{9,}$ ]]; then
  CHAT_ID="-100${CHAT_ID}"
fi

REAL_APK_PATH="$(realpath "$APK_PATH" 2>/dev/null || echo "$APK_PATH")"

echo "Uploading $FILE_NAME ($FILE_SIZE) to Telegram chat: $CHAT_ID (Topic: ${TARGET_TOPIC:-General})..."

CURL_ARGS=(
  -s -S
  --connect-timeout 30
  --max-time 300
  -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendDocument"
  --form-string "chat_id=${CHAT_ID}"
  -F "document=@${REAL_APK_PATH}"
  --form-string "caption=${CAPTION}"
  --form-string "parse_mode=HTML"
)

if [[ -n "${TARGET_TOPIC:-}" ]]; then
  CURL_ARGS+=(--form-string "message_thread_id=${TARGET_TOPIC}")
fi

RESPONSE=$(curl "${CURL_ARGS[@]}")

if echo "$RESPONSE" | grep -q '"ok":true'; then
  echo "::notice ::Artifact successfully forwarded to Telegram!"
else
  echo "::error ::Telegram API error: $RESPONSE"
  exit 1
fi
