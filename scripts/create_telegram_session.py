#!/usr/bin/env python3
"""
One-time helper: create Telegram User API session file.

Reads TELEGRAM_API_ID and TELEGRAM_API_HASH from project .env (or environment).
Session is saved to data/telegram.session (project root).
"""

from __future__ import annotations

import os
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SESSION = PROJECT_ROOT / "data" / "telegram.session"
ENV_FILE = PROJECT_ROOT / ".env"


def load_env_file(path: Path) -> None:
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        os.environ.setdefault(key, value)


def require_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        print(f"Missing {name}. Add it to .env or environment variables.")
        sys.exit(1)
    return value


def main() -> None:
    try:
        from telethon.sync import TelegramClient
    except ImportError:
        print("Telethon is not installed.")
        print("Run: pip install -r scripts/requirements.txt")
        sys.exit(1)

    load_env_file(ENV_FILE)

    api_id = int(require_env("TELEGRAM_API_ID"))
    api_hash = require_env("TELEGRAM_API_HASH")

    session_path = Path(os.environ.get("TELEGRAM_SESSION_PATH", str(DEFAULT_SESSION)))
    if session_path.suffix == ".session":
        session_path = session_path.with_suffix("")

    session_path.parent.mkdir(parents=True, exist_ok=True)

    print(f"Project root: {PROJECT_ROOT}")
    print(f"Session file: {session_path.resolve()}.session")
    print("Telegram will ask for phone number and login code in this terminal.")
    print()

    client = TelegramClient(str(session_path), api_id, api_hash)
    client.start()
    client.disconnect()

    print()
    print("Done. Session saved.")
    print(f"Check: {session_path.resolve()}.session")


if __name__ == "__main__":
    main()
