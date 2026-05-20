# Перенос SQLite-базы на сервер

Инструкция для замены пустой `favorites.db` на сервере уже заполненной базой с локальной машины.

## Что переносить

Один файл (иногда три, если SQLite в WAL-режиме):

| Файл | Обязательно |
|------|-------------|
| `favorites.db` | да |
| `favorites.db-wal` | если есть рядом |
| `favorites.db-shm` | если есть рядом |

Опционально (если используете User session sync):

- `telegram.session` — путь из `TELEGRAM_SESSION_PATH` в `.env`

## Где лежит база локально

Путь задаётся в `.env`:

```env
DATABASE_PATH=./data/favorites.db
```

Это **относительный путь от рабочей директории backend** (обычно папка `backend/` при запуске из IDEA или `gradlew bootRun`).

Типичные места:

- `Telegram_favorits_assist/data/favorites.db` — если в `.env` указано `./data/...` и backend стартует из корня проекта
- `Telegram_favorits_assist/backend/data/favorites.db` — если backend стартует из `backend/`

Проверка на Windows (PowerShell):

```powershell
Get-ChildItem -Recurse -Filter favorites.db E:\1_MyProjects\Telegram_favorits_assist
```

Проверка размера (должен быть заметно больше 0):

```powershell
(Get-Item .\data\favorites.db).Length
```

## Перенос: пошагово

### 1. Остановите приложение локально и на сервере

Backend **обязательно** должен быть остановлен на обеих машинах во время копирования — иначе SQLite-файл может повредиться.

### 2. Сделайте резервную копию на сервере

Если на сервере уже создалась пустая база:

```bash
cd /opt/telegram-favorites/data   # ваш путь
mv favorites.db favorites.db.empty.bak
# wal/shm при наличии:
mv favorites.db-wal favorites.db-wal.bak 2>/dev/null || true
mv favorites.db-shm favorites.db-shm.bak 2>/dev/null || true
```

### 3. Скопируйте файл с локальной машины на сервер

**SCP (из PowerShell / cmd на Windows):**

```powershell
scp E:\1_MyProjects\Telegram_favorits_assist\data\favorites.db user@your-server:/opt/telegram-favorites/data/
```

Если есть `-wal` / `-shm`:

```powershell
scp E:\1_MyProjects\Telegram_favorits_assist\data\favorites.db* user@your-server:/opt/telegram-favorites/data/
```

**Или через WinSCP / FileZilla** — перетащите `favorites.db` в ту же папку, что указана в `DATABASE_PATH` на сервере.

### 4. Настройте `.env` на сервере

Путь в `DATABASE_PATH` должен указывать **на скопированный файл**:

```env
DATABASE_PATH=/opt/telegram-favorites/data/favorites.db
```

Или относительный путь от каталога запуска backend:

```env
DATABASE_PATH=./data/favorites.db
```

Папка для файла должна существовать (backend создаёт её сам при старте, если путь с подпапками).

### 5. Права доступа (Linux)

Пользователь, под которым запускается backend, должен читать и писать базу:

```bash
sudo chown appuser:appuser /opt/telegram-favorites/data/favorites.db
sudo chmod 644 /opt/telegram-favorites/data/favorites.db
```

Если сервис не может писать — импорт и обработка очереди упадут с ошибкой доступа.

### 6. Запустите backend на сервере

```bash
cd backend
./gradlew bootRun
# или java -jar build/libs/....jar
```

Проверка:

```bash
curl http://localhost:8080/api/stats
curl http://localhost:8080/api/services?size=3
```

Должны вернуться ваши `totalServices`, категории и карточки — не нули.

## Важные моменты

### Flyway и миграции

В скопированной базе уже применены миграции (`flyway_schema_history`). При старте Flyway **не пересоздаёт** таблицы — просто сверит версию. Подмена файла целиком — нормальный сценарий.

Не смешивайте: старую базу + новый код с **дополнительными** миграциями, которых не было локально — тогда Flyway применит только новые. Если версии кода одинаковые, проблем не будет.

### Повторный импорт `result.json`

После переноса **не загружайте** тот же `result.json` снова — сообщения уже в базе (будут «пропущены» как дубликаты). Новые данные — только новый экспорт или sync.

### Очередь обработки

В базе могут остаться посты со статусом `PENDING`. Frontend на сервере **автоматически** продолжит обработку пакетами по 50.

### Бэкап перед обновлениями

На сервере периодически копируйте файл:

```bash
cp favorites.db favorites.db.backup-$(date +%Y%m%d)
```

## Откат

Если что-то пошло не так:

```bash
mv favorites.db.empty.bak favorites.db
```

и перезапустите backend.

## Чеклист

- [ ] Backend остановлен локально и на сервере
- [ ] Скопирован `favorites.db` (+ `-wal`/`-shm` при наличии)
- [ ] `DATABASE_PATH` на сервере указывает на этот файл
- [ ] Права на файл позволяют читать/писать процессу backend
- [ ] `/api/stats` показывает ожидаемое число сервисов
- [ ] Каталог в UI отображает карточки
