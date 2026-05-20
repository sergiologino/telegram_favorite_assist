# Архитектура

## Обзор

Монорепозиторий: **backend (Spring Boot)** + **frontend (React SPA)**.

```
┌─────────────────┐     REST/JSON      ┌──────────────────────────┐
│  Frontend (SPA) │ ◄────────────────► │  Backend (Spring Boot)   │
│  React + Vite   │                    │  REST API, Scheduler     │
└─────────────────┘                    └───────────┬──────────────┘
                                                   │
         ┌─────────────────────────────────────────┼─────────────────────────┐
         ▼                     ▼                   ▼                         ▼
  ┌─────────────┐      ┌─────────────┐     ┌─────────────┐          ┌─────────────┐
  │   SQLite    │      │ Telegram    │     │  OpenAI     │          │ HTTP fetch  │
  │ favorites.db│      │ Bot API     │     │  API        │          │ OG, GitHub  │
  └─────────────┘      └─────────────┘     └─────────────┘          └─────────────┘
                              │
                              │  + Telegram Desktop export (result.json)
                              ▼
```

## Источники данных

| Источник | Назначение |
|----------|------------|
| Telegram Desktop export | Первичный импорт истории «Избранное» |
| Telegram User API (Telethon) | Saved Messages, инкрементальный импорт |
| Telegram Bot API | Пересланные сообщения боту |

## Backend модули

| Пакет | Назначение |
|-------|------------|
| `telegram` | Bot sync, export parser |
| `enrichment` | Open Graph, GitHub |
| `classification` | OpenAI + fallback |
| `processing` | Pipeline post → service card |
| `catalog` | REST API |
| `scheduler` | Cron 2×/день |

## API

| Method | Path | Описание |
|--------|------|----------|
| GET | `/api/health` | Healthcheck |
| GET | `/api/services` | Список (`q`, `category`, `from`, `to`, `page`, `size`) |
| GET | `/api/services/{id}` | Деталь |
| GET | `/api/categories` | Категории + count |
| GET | `/api/stats` | Статистика |
| POST | `/api/sync/trigger` | Sync bot + process |
| POST | `/api/import/export` | Upload result.json |

## Структура репозитория

```
Telegram_favorits_assist/
├── backend/
├── frontend/
├── docs/ai/
├── docs/TELEGRAM_SETUP.md
├── .env.example
└── README.md
```
