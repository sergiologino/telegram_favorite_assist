# Finds

**Finds** ([finds.altacod.com](https://finds.altacod.com)) — подборка Altacod: полезные и интересные сервисы, приложения и ресурсы, собранные за последние 4 года из статей, заметок и разных источников.

## Быстрый старт (локально)

### Требования

- Java 17+
- Node.js 20+
- Gradle (через `backend/gradlew.bat`; кэш: `%USERPROFILE%\.gradle`)

### 1. Настройка окружения

```powershell
copy .env.example .env
# Заполните OPENAI_API_KEY и при необходимости ключи синхронизации
```

Подробнее по импорту и синхронизации: [docs/TELEGRAM_SETUP.md](docs/TELEGRAM_SETUP.md)

### 2. Backend

```powershell
cd backend
.\gradlew.bat bootRun
```

API: http://localhost:8080/api/health

### 3. Frontend

```powershell
cd frontend
npm install
npm run dev
```

UI: http://localhost:5173

## Наполнение каталога

Служебная страница **Импорт** в UI и инструкции в [docs/TELEGRAM_SETUP.md](docs/TELEGRAM_SETUP.md) — для загрузки накопленных ссылок и синхронизации новых записей.

## Сборка и тесты

```powershell
# Backend
cd backend
.\gradlew.bat test

# Frontend
cd frontend
npm test
npm run build
```

## Production (кратко)

Production URL: **https://finds.altacod.com**

Переменные окружения:

```env
SITE_URL=https://finds.altacod.com
SITE_NAME=Finds — полезные сервисы и приложения
VITE_SITE_URL=https://finds.altacod.com
```

1. `npm run build` в `frontend/`
2. Скопируйте `frontend/dist/*` в `backend/src/main/resources/static/`
3. Запустите backend JAR
4. Ограничьте доступ (firewall / reverse proxy) — auth в v1 нет

Перенос уже заполненной SQLite-базы с локальной машины: [docs/DATABASE_TRANSFER.md](docs/DATABASE_TRANSFER.md)

## Docker / Coolify

Один контейнер (UI + API + SQLite volume):

```bash
docker compose up --build
```

Подробно: [docs/COOLIFY_DEPLOY.md](docs/COOLIFY_DEPLOY.md)

## Структура

```
backend/     Spring Boot API, sync, classification
frontend/    React UI
docs/ai/     Память проекта для AI-агента
docs/        Инструкции
.env.example   Переменные окружения
```
