# Архитектурные решения

---

## ADR-001 | Accepted | Монорепозиторий: Spring Boot + React

**Решение:** один репозиторий, backend Spring Boot, frontend React + Vite + TypeScript.

**Причина:** серверные секреты, cron, долгие HTTP-запросы.

---

## ADR-002 | Superseded | Telegram User API для «Избранное»

**Было:** обязательный User API (MTProto).

**Superseded by ADR-008.**

---

## ADR-008 | Accepted | Bot API + Export + User session (Telethon)

**Решение:** MVP использует три канала:
1. Telethon User session — автосинхронизация Saved Messages (Java вызывает Python-скрипт)
2. Telegram Desktop export — первичный импорт истории
3. Telegram Bot API — пересылка новых постов боту

**Причина:** Telethon session-файл уже создаётся пользователем; прямой MTProto в Java сложнее и не читает Telethon session.

**Env:** `TELEGRAM_API_ID`, `TELEGRAM_API_HASH`, `TELEGRAM_SESSION_PATH`, `TELEGRAM_USER_API_ENABLED`, `PYTHON_EXECUTABLE`, `APP_PROJECT_ROOT=..`

---

## ADR-003 | Accepted | OpenAI для классификации

**Env:** `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_ENABLED`.

**Fallback:** keyword-based классификация при отсутствии ключа.

---

## ADR-004 | Accepted | SQLite локально

**Решение:** SQLite file (`DATABASE_PATH=./data/favorites.db`).

**Причина:** простой локальный запуск; PostgreSQL — при деплое (не реализовано).

---

## ADR-005 | Accepted | Без авторизации в v1

Личное приложение, localhost / приватная сеть.

---

## ADR-006 | Accepted | Планировщик Spring `@Scheduled`

**Cron:** `0 0 8,20 * * *` (08:00 и 20:00).

**Env:** `SYNC_CRON`, `APP_TIMEZONE=Europe/Moscow`.

---

## ADR-007 | Cancelled | Java MTProto библиотека

**Статус:** Cancelled — MVP обходится без User API.

---

## ADR-009 | Accepted | Flyway + ddl-auto=none

**Причина:** SQLite INTEGER vs Hibernate BIGINT — схема только через Flyway.
