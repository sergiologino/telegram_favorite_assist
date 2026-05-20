#!/usr/bin/env python3
"""
Fetch new messages from Telegram Saved Messages (chat 'me') using User API session.

Outputs JSON to stdout:
{"messages":[{"id":1,"date":"...","text":"..."}],"maxId":123}
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from telegram_env import create_client, load_env_file, resolve_session_path


def extract_text(message) -> str:
    text = message.message or message.text or ""
    if text:
        return text.strip()

    parts: list[str] = []
    if message.media and getattr(message.media, "webpage", None):
        webpage = message.media.webpage
        if getattr(webpage, "title", None):
            parts.append(str(webpage.title))
        if getattr(webpage, "description", None):
            parts.append(str(webpage.description))
        if getattr(webpage, "url", None):
            parts.append(str(webpage.url))
    return "\n".join(part for part in parts if part).strip()


def main() -> None:
    parser = argparse.ArgumentParser(description="Sync Telegram Saved Messages")
    parser.add_argument("--since-id", type=int, default=0, help="Import messages with id greater than this value")
    parser.add_argument("--limit", type=int, default=500, help="Maximum number of messages to fetch")
    args = parser.parse_args()

    try:
        load_env_file()
        session_path = resolve_session_path()
        if not Path(f"{session_path}.session").exists():
            print(
                f"Session file not found: {session_path}.session\n"
                "Run: python scripts/create_telegram_session.py",
                file=sys.stderr,
            )
            sys.exit(2)

        client = create_client()
        client.connect()
        if not client.is_user_authorized():
            print("Telegram session is not authorized. Recreate session file.", file=sys.stderr)
            sys.exit(2)

        messages = []
        max_id = args.since_id

        for message in client.iter_messages("me", min_id=args.since_id, reverse=True, limit=args.limit):
            text = extract_text(message)
            if not text:
                continue
            messages.append(
                {
                    "id": message.id,
                    "date": message.date.isoformat(),
                    "text": text,
                }
            )
            if message.id > max_id:
                max_id = message.id

        client.disconnect()
        json.dump({"messages": messages, "maxId": max_id}, sys.stdout, ensure_ascii=False)
    except Exception as ex:
        print(str(ex), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
