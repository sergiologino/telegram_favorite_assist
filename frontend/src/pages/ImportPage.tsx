import { ChangeEvent, useState } from 'react';
import { api, SyncResponse } from '../api/client';
import { useQueueProcessing } from '../hooks/useQueueProcessing';
import PageSeo from '../seo/PageSeo';
import { buildWebPageJsonLd } from '../seo/structuredData';

const IMPORT_DESCRIPTION =
  'Служебная страница импорта ссылок и синхронизации для наполнения Finds.';

export default function ImportPage() {
  const [result, setResult] = useState<SyncResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { stats, refreshStats } = useQueueProcessing();

  async function handleExportImport(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;

    setLoading(true);
    setError(null);
    try {
      const response = await api.importExport(file);
      await refreshStats();
      setResult(response);
    } catch (importError) {
      setError(importError instanceof Error ? importError.message : 'Ошибка импорта');
    } finally {
      setLoading(false);
      event.target.value = '';
    }
  }

  async function handleSync() {
    setLoading(true);
    setError(null);
    try {
      const response = await api.triggerSync();
      await refreshStats();
      setResult(response);
    } catch (syncError) {
      setError(syncError instanceof Error ? syncError.message : 'Ошибка синхронизации');
    } finally {
      setLoading(false);
    }
  }

  const hasExportResult =
    (result?.exportImported ?? 0) > 0 ||
    (result?.exportSkippedDuplicate ?? 0) > 0 ||
    (result?.exportSkippedEmpty ?? 0) > 0;

  return (
    <>
      <PageSeo
        title="Импорт"
        description={IMPORT_DESCRIPTION}
        path="/import"
        jsonLd={[buildWebPageJsonLd('Импорт', IMPORT_DESCRIPTION, '/import')]}
        noindex
      />

      <header className="page-header">
        <h1>Импорт ссылок</h1>
        <p className="page-lead">
          Загрузите JSON-экспорт или запустите синхронизацию — Finds разберёт ссылки и добавит их в каталог.
        </p>
      </header>

      <section className="panel">
        <h2>Загрузка result.json</h2>
        <p className="muted">
          Выберите JSON-файл с накопленными ссылками и заметками. Подробная инструкция по формату экспорта — в{' '}
          <code>docs/TELEGRAM_SETUP.md</code>.
        </p>
        <div className="import-actions">
          <label className="button secondary">
            Загрузить result.json
            <input type="file" accept="application/json,.json" hidden onChange={handleExportImport} disabled={loading} />
          </label>
          <button className="primary" type="button" onClick={handleSync} disabled={loading}>
            Синхронизировать
          </button>
        </div>
      </section>

      <section className="panel">
        <h2>Способы синхронизации</h2>
        <ol>
          <li>
            <strong>User session</strong> — если указаны API ID, API hash и файл session, приложение подтягивает новые
            записи автоматически.
          </li>
          <li>
            <strong>Bot API</strong> — пересылайте новые сообщения боту (нужен токен бота в настройках).
          </li>
          <li>Обработка очереди запускается автоматически после импорта.</li>
        </ol>
      </section>

      <section className="panel faq-section">
        <h2>Частые вопросы</h2>
        <dl>
          <dt>Как добавить ссылки пакетом?</dt>
          <dd>Загрузите `result.json` на этой странице — дубликаты будут пропущены.</dd>
          <dt>Нужны ли медиа-папки из экспорта?</dt>
          <dd>Нет, достаточно `result.json` — ссылки извлекаются из текста и метаданных.</dd>
          <dt>Что делать после импорта?</dt>
          <dd>Дождитесь автоматической обработки очереди и откройте каталог — карточки появятся по мере готовности.</dd>
        </dl>
      </section>

      {loading && <div className="empty-state">Обработка...</div>}
      {error && <div className="error-state">{error}</div>}
      {result && (
        <section className="panel">
          <h2>Результат</h2>
          {hasExportResult && (
            <>
              <p>Экспорт: импортировано {result.exportImported}</p>
              {result.exportSkippedDuplicate > 0 && (
                <p className="muted">
                  Уже в базе (повторная загрузка): {result.exportSkippedDuplicate}
                </p>
              )}
              {result.exportSkippedEmpty > 0 && (
                <p className="muted">
                  Без текста и ссылок (фото, стикеры и т.п.): {result.exportSkippedEmpty}
                </p>
              )}
            </>
          )}
          {(result.userImported > 0 || result.userSkipped > 0) && (
            <p>
              User session: импортировано {result.userImported}, пропущено {result.userSkipped}
            </p>
          )}
          {result.userError && <p className="error-state">{result.userError}</p>}
          {(result.botImported > 0 || result.botSkipped > 0) && (
            <p>
              Bot API: импортировано {result.botImported}, пропущено {result.botSkipped}
            </p>
          )}
          {result.botError && <p className="error-state">{result.botError}</p>}
          {(result.processed > 0 || result.failed > 0 || result.skipped > 0) && (
            <p>
              Обработано: {result.processed}, ошибок: {result.failed}, пропущено: {result.skipped}
            </p>
          )}
          {stats && (
            <p>
              В очереди на обработку: {stats.pendingPosts}, в каталоге: {stats.totalServices}
            </p>
          )}
        </section>
      )}
    </>
  );
}
