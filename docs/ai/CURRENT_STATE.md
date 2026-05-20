# Текущее состояние

**Обновлено:** 2026-05-20

## Статус проекта

**MVP реализован** — backend + frontend работают локально.

## Реализовано

### Backend (Spring Boot 3, Java 17, SQLite)

- Импорт Telegram Desktop export (`POST /api/import/export`) — текст, `text_link`/`href`, `web_page`; загрузка в очередь без блокирующей обработки
- Синхронизация через Bot API (`getUpdates`, `POST /api/sync/trigger`)
- **User session sync** — чтение «Избранное» через Telethon session (`TelegramUserSessionService`, `scripts/sync_saved_messages.py`)
- Планировщик 2×/день (08:00, 20:00, timezone из env)
- Open Graph enrichment, GitHub stars
- OpenAI классификация + fallback без ключа
- REST API: services, categories, stats, health
- Flyway миграции, JUnit тесты (`gradlew test` проходит)

### Frontend (React + Vite + TypeScript)

- Каталог с карточками сервисов
- Поиск, фильтр по категории и дате
- Детальная карточка
- Страница импорта (export + sync bot)
- Адаптивная вёрстка (mobile bottom nav)
- Vitest тесты (`npm test` проходит)

### Документация

- `README.md` — запуск
- `docs/TELEGRAM_SETUP.md` — Bot API + экспорт + инструкция User API
- `.env.example`

## Способ получения данных из «Избранное»

1. **User session (Telethon)** — автосинхронизация Saved Messages (рекомендуется)
2. **Экспорт** Telegram Desktop → `result.json` (первая загрузка истории)
3. **Пересылка боту** (дополнительный канал)

## Запуск

```powershell
# Terminal 1
cd backend && .\gradlew.bat bootRun

# Terminal 2
cd frontend && npm run dev
```

## Следующие шаги (не сделано)

- Auth при публичном деплое
- Ручное редактирование категорий
- Production: static frontend в Spring Boot + Docker
