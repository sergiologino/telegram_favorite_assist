## 2026-05-20 (Import 500 fix)

- Исправлено хранение `Instant` в SQLite: ISO-8601 + чтение legacy epoch millis (`InstantAttributeConverter`).
- Импорт `result.json` больше не запускает полную обработку в том же HTTP-запросе — только загрузка в очередь.
- Обработка очереди батчами по 50 (`app.sync.process-batch-size`) через «Синхронизировать» или планировщик.

## 2026-05-20 (SQLite startup)

- Автосоздание папки для SQLite БД при старте; default `DATABASE_PATH=../data/favorites.db`.

## 2026-05-19 (Export parser)

- Импорт `result.json` подхватывает `href`, `text_link` и `media_type: web_page` (без медиа-папок).

## 2026-05-19 (User session)

- Реализована синхронизация Saved Messages через Telethon session (`TelegramUserSessionService`, `scripts/sync_saved_messages.py`).

## 2026-05-19 (Gradle)

- Backend переведён с Maven на Gradle (`gradlew.bat`, Gradle 8.10.2, кэш `%USERPROFILE%\.gradle`).
- Добавлен `scripts/create_telegram_session.py` для создания User API session из IDEA/терминала.

## 2026-05-19 (MVP)

- Реализован монорепо: Spring Boot backend + React frontend.
- Импорт через Telegram Desktop export + синхронизация через Bot API (без User API).
- OpenAI классификация, Open Graph, GitHub stars, UI каталога, README и TELEGRAM_SETUP.
