# Настройка Telegram

User API **реализован** через Telethon session-файл. Backend вызывает `scripts/sync_saved_messages.py` при синхронизации (cron и `POST /api/sync/trigger`).

Приложение работает **без User API** (достаточно Bot API + экспорт). User API — рекомендуемый способ автосинхронизации «Избранное».

---

## Часть 1. Bot API (основной способ для новых постов)

### Шаг 1. Создать бота

1. Откройте [@BotFather](https://t.me/BotFather) в Telegram
2. Отправьте `/newbot`
3. Укажите имя и username (должен заканчиваться на `bot`)
4. Сохраните **токен** вида `123456789:AAH...`

### Шаг 2. Добавить токен в `.env`

```env
TELEGRAM_BOT_TOKEN=123456789:AAHxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TELEGRAM_BOT_ENABLED=true
```

### Шаг 3. Активировать бота

1. Найдите бота по username
2. Нажмите **Start** (`/start`)
3. Пересылайте в него сообщения из «Избранное»

### Шаг 4. Проверить синхронизацию

- В UI: **Импорт** → «Синхронизировать с ботом»
- Или API: `POST http://localhost:8080/api/sync/trigger`

---

## Часть 2. Экспорт «Избранное» (первая загрузка истории)

### Telegram Desktop

1. **Settings** → **Advanced** → **Export Telegram data**
2. Выберите только **Saved Messages** / **Избранное**
3. Формат: **JSON**
4. Дождитесь архива, найдите `result.json`
5. Загрузите в UI: **Импорт** → «Загрузить result.json»

> Папки с медиа (`photos/`, `video_files/` и т.д.) загружать **не нужно**. Импортируется только `result.json`: текст, `text_link`/`href` и превью ссылок (`media_type: web_page`). Open Graph подтягивается с сайтов по URL.

---

## Часть 3. Telegram User API (автосинхронизация «Избранное»)

Backend читает Saved Messages через session-файл Telethon (`data/telegram.session`).

**Требования:** Python 3, `pip install -r scripts/requirements.txt`, заполненный `.env`.

### 3.1. Получить api_id и api_hash

1. Откройте https://my.telegram.org
2. Войдите по номеру телефона (код придёт в Telegram)
3. Перейдите в **API development tools**
4. Создайте приложение:
   - **App title**: любое (например `Favorites Assist`)
   - **Short name**: латиница, 5–32 символа (например `favassist`)
   - **Platform**: Desktop
5. Сохраните:
   - **api_id** — число
   - **api_hash** — строка

### 3.2. User session (авторизация аккаунта)

User session — это результат входа вашим **личным** аккаунтом через MTProto (не бот).

В проекте есть готовый скрипт: `scripts/create_telegram_session.py`.

#### Запуск из IntelliJ IDEA (рекомендуется)

1. Заполните в `.env` (корень проекта):
   ```env
   TELEGRAM_API_ID=12345678
   TELEGRAM_API_HASH=your_api_hash
   TELEGRAM_SESSION_PATH=./data/telegram.session
   ```
2. Откройте в IDEA **Terminal** (`Alt+F12`).
3. Убедитесь, что вы в **корне проекта**:
   ```powershell
   cd E:\1_MyProjects\Telegram_favorits_assist
   Get-Location
   ```
4. Один раз установите Telethon:
   ```powershell
   pip install -r scripts/requirements.txt
   ```
5. Запустите скрипт:
   ```powershell
   python scripts/create_telegram_session.py
   ```
6. В этом же терминале введите:
   - номер телефона (международный формат, например `+79001234567`)
   - код из Telegram
   - пароль 2FA (если включён)

Файл сессии появится здесь:

```text
E:\1_MyProjects\Telegram_favorits_assist\data\telegram.session
```

#### Альтернатива: Run Configuration в IDEA

Если установлен Python plugin:

1. ПКМ по `scripts/create_telegram_session.py` → **Run**
2. **Run → Edit Configurations…**
3. **Working directory:** `E:\1_MyProjects\Telegram_favorits_assist` (корень проекта)
4. **Environment variables:** можно не задавать, если есть `.env`

> Важно: **Working directory** должна быть корнем проекта, иначе `.env` и `data/` будут искаться не там.

#### Где искать файл сессии

Скрипт сохраняет сессию по пути из `TELEGRAM_SESSION_PATH` (по умолчанию `./data/telegram.session` относительно **корня проекта**).

| Способ запуска | Где будет файл |
|----------------|----------------|
| Терминал IDEA из корня проекта | `...\Telegram_favorits_assist\data\telegram.session` |
| Run Configuration с Working directory = корень проекта | то же самое |
| Запуск из другой папки без настройки | `.env` может не найтись — используйте корень проекта |

Дополнительно может появиться `telegram.session-journal` — его тоже не коммитить.

#### Пример кода (если нужен вручную)

```python
from pathlib import Path
from telethon.sync import TelegramClient

api_id = 12345678
api_hash = "your_api_hash"
session_path = Path("./data/favorites_session")
session_path.parent.mkdir(parents=True, exist_ok=True)

client = TelegramClient(str(session_path), api_id, api_hash)
client.start()
print(f"Session saved to {session_path.resolve()}.session")
```

### 3.3. Переменные для будущей интеграции

```env
TELEGRAM_API_ID=12345678
TELEGRAM_API_HASH=0123456789abcdef0123456789abcdef
TELEGRAM_SESSION_PATH=./data/telegram.session
```

### Ограничения User API

- Это **ваш личный аккаунт**, не бот
- Сессию нельзя публиковать и коммитить в git
- Telegram может запросить 2FA-пароль
- При смене устройства может потребоваться повторная авторизация

---

## OpenAI (классификация)

```env
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
OPENAI_ENABLED=true
```

Без ключа приложение использует fallback-классификацию по ключевым словам.

## GitHub (опционально, для stars)

```env
GITHUB_TOKEN=ghp_...
```

Без токена GitHub API работает с лимитом; для личного использования обычно достаточно.
