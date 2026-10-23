# Telegram Group With Topics Setup

This guide walks you through setting up automatic artifact forwarding to your **Telegram Group with Forum Topics**.

---

## 1. Create Your Telegram Bot

1. Open Telegram and message [@BotFather](https://t.me/BotFather).
2. Send `/newbot` and follow the instructions to choose a display name and username (e.g. `SmartIslandBuildsBot`).
3. BotFather will provide an API Token:
   ```text
   123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ
   ```
   Save this token as your `TELEGRAM_BOT_TOKEN`.

---

## 2. Add the Bot to Your Telegram Group

1. Open your Telegram Group (with Topics enabled).
2. Go to **Group Info** -> **Administrators** -> **Add Administrator**.
3. Search for your bot and add it.
4. Ensure the bot has the following admin permissions:
   - ✅ **Post Messages** / **Send Messages**
   - ✅ **Manage Topics** (so it can post in forum topics)

---

## 3. How to Get Chat ID and Topic ID (in 5 Seconds)

In Telegram Desktop, Android, iOS, or Web:

1. Right-click or long-press on the topic where you want builds posted.
2. Select **Copy Link to Topic**.
3. Look at the copied link:
   ```text
   https://t.me/c/2345678901/42
   ```
   - **Chat ID:** `-1002345678901` *(the first number, prefixed with `-100`)*
     > Note: If you paste just `2345678901`, our forwarding script automatically prefixes `-100` for you.
   - **Topic ID:** `42` *(the number at the end)*

---

## 4. Recommended Multi-Topic Setup

Most communities prefer keeping different event types in separate topics:

| Topic | Purpose | Secret to Set |
|---|---|---|
| `#releases` | Signed official release APKs | `TELEGRAM_RELEASE_TOPIC_ID` |
| `#dev-builds` | Dev/testing debug APKs | `TELEGRAM_DEBUG_TOPIC_ID` |
| `#github-activity` | Issues, PRs, stars notifications | `TELEGRAM_EVENTS_TOPIC_ID` |
| General (fallback) | Everything in one topic | `TELEGRAM_TOPIC_ID` |

---

## 5. Add GitHub Repository Secrets

In your GitHub repository, open:
```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

Add the following:

| Secret Name | Required? | Example | Description |
|---|---|---|---|
| `TELEGRAM_BOT_TOKEN` | **Yes** | `123456789:ABCdef...` | Token from @BotFather |
| `TELEGRAM_CHAT_ID` | **Yes** | `-1002345678901` | Your supergroup ID |
| `TELEGRAM_TOPIC_ID` | Optional | `1` | Fallback / default topic ID |
| `TELEGRAM_RELEASE_TOPIC_ID` | Optional | `10` | Topic for Release APKs |
| `TELEGRAM_DEBUG_TOPIC_ID` | Optional | `42` | Topic for Debug APKs |
| `TELEGRAM_EVENTS_TOPIC_ID` | Optional | `55` | Topic for issues, PRs, and stars |

---

## 6. What Gets Forwarded

### Build Artifacts (`.github/workflows/build.yml` & `.github/workflows/release.yml`)
- **Release APKs** — On tag `v*` or manual dispatch: signed APK with version, changelog link, recent commits, download links, and project info.
- **Debug APKs** — On `dev` push or manual dispatch: debug APK with branch, author, recent commits, and CI run links.

### GitHub Events (`.github/workflows/telegram-notifications.yml`)
- **🐛 New Issue** — Issue number, title, author profile link, type detection (Bug / Feature / Question), labels, and description preview.
- **🔀 New PR** — PR number, title, author, target branch, additions/deletions/files changed, labels, and description preview.
- **✅ PR Merged** — PR number, title, author, who merged it, target branch, commit count, and diff stats.
- **⭐ New Star** — Who starred, their bio/location/followers/repos, current total star count, and milestone alerts (10, 25, 50, 100, 250, 500, 1000+).

### Safe Fallback
If the Telegram secrets are not configured, all workflows gracefully skip notifications without failing CI.

