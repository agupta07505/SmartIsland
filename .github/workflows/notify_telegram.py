#!/usr/bin/env python3
#
# Smart Island (2026)
# © Animesh Gupta — github.com/agupta07505
# Licensed under the GNU GPL v3 License
#
# Sends a "new commit, building…" notification to a Telegram group/topic
# before the actual build starts.

import os
import re
import subprocess
import requests


def git(fmt):
    return subprocess.check_output(
        ["git", "log", "-1", f"--pretty=format:{fmt}"]
    ).decode("utf-8")


def escape_md2(text):
    return re.sub(r"([_~`#\+\-=|{}.!])", r"\\\1", text)


def escape_parens(text):
    return re.sub(r"([()])", r"\\\1", text)


def main():
    bot_token = os.environ["BOT_TOKEN"]
    chat_id = os.environ["CHAT_ID"]
    topic_id = os.environ.get("TOPIC_ID")

    author = git("%an")
    message = git("%s")
    sha = git("%H")
    sha_short = git("%h")

    repo = os.environ.get("GITHUB_REPOSITORY", "agupta07505/SmartIsland")

    text = (
        f"A new [commit](https://github.com/{repo}/commit/{sha}) "
        f"has been merged to the repository by *{author}*.\n\n"
        f"*What has changed:*\n>{escape_parens(message)}\n\n"
        f"Building now — APK will arrive here shortly if the build passes.\n\n"
        f"#{sha_short}"
    )

    payload = {
        "chat_id": chat_id,
        "text": escape_md2(text),
        "parse_mode": "MarkdownV2",
        "disable_web_page_preview": True,
    }
    if topic_id:
        payload["message_thread_id"] = topic_id

    resp = requests.post(
        f"https://api.telegram.org/bot{bot_token}/sendMessage", json=payload
    )
    if resp.status_code != 200:
        print(f"Failed to send message: {resp.status_code} {resp.text}")
    else:
        print("Message sent successfully.")


if __name__ == "__main__":
    main()
