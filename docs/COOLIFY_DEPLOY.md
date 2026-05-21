# Деплой в Coolify (один контейнер)

Приложение собирается в **один Docker-образ**: React UI встроен в Spring Boot JAR, наружу торчит только порт **8080**.

## Что понадобится

- Репозиторий в Git (GitHub / GitLab / Gitea)
- Coolify на локальном сервере
- Переменные из `.env` (ключи OpenAI, Telegram bot и т.д.)
- **Persistent volume** для SQLite (`/data`)

## 1. Подготовка репозитория

Убедитесь, что в Git есть:

- `Dockerfile` (в корне)
- `backend/`, `frontend/`
- **нет** файла `.env` и `data/favorites.db` (секреты и база — только на сервере)

Секреты задаются в Coolify, не в Git.

## 2. Новый ресурс в Coolify

1. **+ New Resource** → **Application**
2. Источник: ваш Git-репозиторий, ветка `main` / `master`
3. **Build Pack:** `Dockerfile`
4. **Dockerfile location:** `/Dockerfile` (корень репо)
5. **Port:** `8080`
6. **Healthcheck path:** `/api/health` (ожидается ответ `ok`)

## 3. Persistent Storage (обязательно)

Без volume база SQLite будет **теряться** при каждом redeploy.

| Mount path (в контейнере) | Описание |
|---------------------------|----------|
| `/data`                   | SQLite `favorites.db` |

В Coolify: **Storages** → Add Volume → **Mount Path** `/data`.

## 4. Переменные окружения

Скопируйте из `.env.example` и заполните в Coolify → **Environment Variables**:

```env
SERVER_PORT=8080
DATABASE_PATH=/data/favorites.db
APP_PROJECT_ROOT=/app
APP_TIMEZONE=Europe/Moscow
SITE_URL=https://finds.altacod.com
SITE_NAME=Finds — полезные сервисы и приложения

OPENAI_API_KEY=
OPENAI_MODEL=gpt-4o-mini
OPENAI_ENABLED=false

AI_INTEGRATION_ENABLED=true
AI_INTEGRATION_BASE_URL=https://your-ai-integration-host
AI_INTEGRATION_API_KEY=aikey_...
AI_INTEGRATION_ADMIN_USERNAME=admin
AI_INTEGRATION_ADMIN_PASSWORD=admin
AI_INTEGRATION_OWNER_EMAIL=admin@example.com

TELEGRAM_BOT_TOKEN=...
TELEGRAM_BOT_ENABLED=true

TELEGRAM_USER_API_ENABLED=false

GITHUB_TOKEN=

SYNC_CRON=0 0 8,20 * * *
SYNC_PROCESS_BATCH_SIZE=50
```

**Build argument** (Coolify → Docker Build Arguments):

```env
VITE_SITE_URL=https://finds.altacod.com
VITE_YANDEX_METRIKA_ID=109312391
```

`SITE_URL` — для `robots.txt` и `sitemap.xml` на backend.  
`VITE_SITE_URL` — для canonical и Open Graph в SPA (задаётся **на этапе сборки** образа).  
`VITE_YANDEX_METRIKA_ID` — ID счётчика Яндекс.Метрики (тоже на этапе сборки).

`TELEGRAM_USER_API_ENABLED=false` — в образе **нет Python/Telethon**. User session sync в контейнере не работает без доработки образа. Используйте **экспорт JSON** и **Bot API**.

## 5. Перенос уже заполненной базы

1. Остановите приложение в Coolify
2. Загрузите `favorites.db` в volume `/data` на сервере (см. [DATABASE_TRANSFER.md](DATABASE_TRANSFER.md))
3. Запустите снова

Или после первого деплоя подмените файл в примонтированном томе.

## 6. Домен и HTTPS

В Coolify укажите домен **finds.altacod.com** → Coolify сам выпустит Let's Encrypt.

Приложение отдаёт и API (`/api/*`), и UI с одного порта — **отдельный frontend-контейнер не нужен**.

## 7. Проверка после деплоя

```bash
curl https://finds.altacod.com/api/health
curl https://finds.altacod.com/api/stats
curl https://finds.altacod.com/robots.txt
curl https://finds.altacod.com/sitemap.xml
```

Откройте UI в браузере — каталог, импорт, автообработка очереди.

## Локальная проверка образа

```bash
docker compose up --build
```

UI: http://localhost:8080  
API: http://localhost:8080/api/health

## Обновление

Push в Git → Coolify пересобирает образ → redeploy.

Volume `/data` сохраняется — данные не пропадают.

## Ограничения v1

- Нет встроенной авторизации — ограничьте доступ firewall / VPN / Basic Auth на reverse proxy
- Импорт `result.json` до **100 MB** (настроено в Spring)
- User API (Telethon) в стандартном образе отключён
