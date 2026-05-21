# Одноразовое объединение категорий

Скрипт встроен в backend и запускается **один раз** при старте приложения, если включена переменная окружения.

## Зачем

После первичной классификации могло накопиться сотни узких категорий (`Бизнес-планирование`, `Анализ рынка` и т.п.). Для фильтра в UI нужно **10–20 широких тематик**. Точная фильтрация — через **теги**.

## Как работает

1. Берёт все элементы каталога (title, description, tags).
2. OpenAI (через **noteapp-ai-integration**) предлагает до 20 **широких** категорий по содержимому элементов.
3. Пакетами переназначает каждый элемент в подходящую категорию (с учётом контента, а не старого названия).
4. Удаляет категории без элементов.

## Запуск (production)

1. Убедитесь, что настроен **AI Integration** (см. `.env.example`) или локально `OPENAI_API_KEY`.
2. В Coolify временно добавьте:

```env
CATEGORY_CONSOLIDATION_ENABLED=true
CATEGORY_CONSOLIDATION_MAX_CATEGORIES=20
CATEGORY_CONSOLIDATION_BATCH_SIZE=15
```

3. Redeploy / restart приложения.
4. Следите за логами: `Category consolidation started` → `Category consolidation finished`.
5. **Сразу после успешного прогона** верните:

```env
CATEGORY_CONSOLIDATION_ENABLED=false
```

и снова redeploy.

> Скрипт стартует в фоне, чтобы не блокировать healthcheck.

## После объединения

Новые элементы каталога по-прежнему классифицируются через OpenAI. В промпт передаётся **список существующих категорий** — модель сначала выбирает из них, и только при необходимости создаёт новую.

## Оценка времени и стоимости

~900 элементов / batch 15 ≈ 60 запросов к AI + 1 запрос на предложение категорий. Время — от нескольких минут.

## AI Integration (production)

На сервере OpenAI может быть недоступен по региону. Используйте сервис **noteapp-ai-integration**:

```env
AI_INTEGRATION_ENABLED=true
AI_INTEGRATION_BASE_URL=https://your-ai-integration-host
AI_INTEGRATION_API_KEY=aikey_...
AI_INTEGRATION_ADMIN_USERNAME=admin
AI_INTEGRATION_ADMIN_PASSWORD=admin
AI_INTEGRATION_OWNER_EMAIL=admin@example.com
OPENAI_ENABLED=false
```

При старте Finds (как AltaPens) логинится **админом** интеграции (`POST /api/auth/login`) и привязывает клиента по API-ключу к владельцу (`POST /api/admin/clients/{id}/assign-user`). Для AI-запросов используется только `X-API-Key` + строковый `userId` (по умолчанию `finds-catalog`).
