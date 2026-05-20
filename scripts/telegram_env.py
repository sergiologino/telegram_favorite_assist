"""Shared helpers for Telegram User API scripts."""

from __future__ import annotations

import os
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
ENV_FILE = PROJECT_ROOT / ".env"
DEFAULT_SESSION = PROJECT_ROOT / "data" / "telegram.session"


def load_env_file(path: Path = ENV_FILE) -> None:
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
        raise RuntimeError(f"Missing {name}. Add it to .env or environment variables.")
    return value


def resolve_session_path() -> Path:
    raw = os.environ.get("TELEGRAM_SESSION_PATH", str(DEFAULT_SESSION)).strip()
    path = Path(raw)
    if not path.is_absolute():
        path = PROJECT_ROOT / path
    if path.suffix == ".session":
        path = path.with_suffix("")
    return path


def create_client():
    from telethon.sync import TelegramClient

    load_env_file()
    api_id = int(require_env("TELEGRAM_API_ID"))
    api_hash = require_env("TELEGRAM_API_HASH")
    session_path = resolve_session_path()
    session_path.parent.mkdir(parents=True, exist_ok=True)
    return TelegramClient(str(session_path), api_id, api_hash)
