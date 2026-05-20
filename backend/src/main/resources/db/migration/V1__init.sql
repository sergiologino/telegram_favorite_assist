CREATE TABLE sync_state (
    id INTEGER PRIMARY KEY,
    last_update_id BIGINT NOT NULL DEFAULT 0
);

INSERT INTO sync_state (id, last_update_id) VALUES (1, 0);

CREATE TABLE categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    slug TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE telegram_posts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    telegram_message_id BIGINT NOT NULL UNIQUE,
    text_content TEXT,
    posted_at TEXT NOT NULL,
    source TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE service_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT,
    image_url TEXT,
    app_url TEXT,
    repo_url TEXT,
    github_stars INTEGER,
    category_id INTEGER REFERENCES categories(id),
    tags TEXT,
    posted_at TEXT,
    telegram_post_id INTEGER REFERENCES telegram_posts(id),
    search_text TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX idx_telegram_posts_status ON telegram_posts(status);
CREATE INDEX idx_telegram_posts_posted_at ON telegram_posts(posted_at);
CREATE INDEX idx_service_items_posted_at ON service_items(posted_at);
CREATE INDEX idx_service_items_category_id ON service_items(category_id);
CREATE INDEX idx_service_items_search_text ON service_items(search_text);
