import { useQueueProcessing } from '../hooks/useQueueProcessing';

export default function QueueProcessingBanner() {
  const { stats, isProcessing, rangeFrom, rangeTo, error, lastResult } = useQueueProcessing();

  const pendingCount = stats?.pendingPosts ?? 0;
  if (pendingCount <= 0 && !isProcessing && !error) {
    return null;
  }

  let statusText = `В очереди ${pendingCount} сообщений. Обработка запускается автоматически.`;
  if (isProcessing && rangeFrom != null && rangeTo != null) {
    statusText = `Обрабатываются с ${rangeFrom} по ${rangeTo} позиции`;
  } else if (!isProcessing && pendingCount > 0) {
    statusText = `В очереди ${pendingCount} сообщений. Следующий пакет скоро начнётся…`;
  } else if (!isProcessing && pendingCount === 0 && lastResult) {
    statusText = `Обработка завершена. В каталоге ${lastResult.totalServices} сервисов.`;
  }

  return (
    <section className="panel queue-banner">
      <p>{statusText}</p>
      {error && <p className="error-state">{error}</p>}
      {lastResult && isProcessing && (
        <p className="muted">
          Последний пакет: +{lastResult.processed} в каталог, ошибок {lastResult.failed}
        </p>
      )}
    </section>
  );
}
