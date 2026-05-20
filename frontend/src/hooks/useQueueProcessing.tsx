import { createContext, ReactNode, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { api, ProcessResponse, Stats } from '../api/client';

const BATCH_SIZE = 50;
const IDLE_POLL_MS = 5000;

type QueueProcessingState = {
  stats: Stats | null;
  isProcessing: boolean;
  rangeFrom: number | null;
  rangeTo: number | null;
  lastResult: ProcessResponse | null;
  error: string | null;
  batchVersion: number;
};

type QueueProcessingContextValue = QueueProcessingState & {
  refreshStats: () => Promise<Stats>;
};

const QueueProcessingContext = createContext<QueueProcessingContextValue | null>(null);

function batchRangeTo(pendingBefore: number): number {
  return Math.max(pendingBefore - BATCH_SIZE, 0);
}

export function QueueProcessingProvider({ children }: { children: ReactNode }) {
  const [stats, setStats] = useState<Stats | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [rangeFrom, setRangeFrom] = useState<number | null>(null);
  const [rangeTo, setRangeTo] = useState<number | null>(null);
  const [lastResult, setLastResult] = useState<ProcessResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [batchVersion, setBatchVersion] = useState(0);
  const runningRef = useRef(false);
  const mountedRef = useRef(true);

  const refreshStats = useCallback(async () => {
    const nextStats = await api.getStats();
    if (mountedRef.current) {
      setStats(nextStats);
    }
    return nextStats;
  }, []);

  const runProcessingLoop = useCallback(async () => {
    if (runningRef.current) {
      return;
    }

    runningRef.current = true;
    setError(null);

    try {
      while (mountedRef.current) {
        const currentStats = await refreshStats();
        if (currentStats.pendingPosts <= 0) {
          setRangeFrom(null);
          setRangeTo(null);
          break;
        }

        const from = currentStats.pendingPosts;
        const estimatedTo = batchRangeTo(from);
        setIsProcessing(true);
        setRangeFrom(from);
        setRangeTo(estimatedTo);

        const result = await api.processPending();
        if (!mountedRef.current) {
          break;
        }

        setLastResult(result);
        setRangeFrom(result.pendingBefore);
        setRangeTo(result.pendingRemaining);
        setStats(await refreshStats());
        setBatchVersion((value) => value + 1);

        if (result.processed === 0 && result.failed === 0 && result.skipped === 0) {
          setError('Очередь не продвигается — обработка остановлена');
          break;
        }

        if (result.pendingRemaining <= 0) {
          setRangeFrom(null);
          setRangeTo(null);
          break;
        }
      }
    } catch (processError) {
      if (mountedRef.current) {
        setError(processError instanceof Error ? processError.message : 'Ошибка обработки очереди');
      }
    } finally {
      if (mountedRef.current) {
        setIsProcessing(false);
      }
      runningRef.current = false;
    }
  }, [refreshStats]);

  useEffect(() => {
    mountedRef.current = true;
    refreshStats().then((initialStats) => {
      if (initialStats.pendingPosts > 0) {
        runProcessingLoop();
      }
    });

    const pollId = window.setInterval(() => {
      if (runningRef.current) {
        return;
      }
      refreshStats().then((nextStats) => {
        if (nextStats.pendingPosts > 0) {
          runProcessingLoop();
        }
      });
    }, IDLE_POLL_MS);

    return () => {
      mountedRef.current = false;
      window.clearInterval(pollId);
    };
  }, [refreshStats, runProcessingLoop]);

  const value: QueueProcessingContextValue = {
    stats,
    isProcessing,
    rangeFrom,
    rangeTo,
    lastResult,
    error,
    batchVersion,
    refreshStats,
  };

  return <QueueProcessingContext.Provider value={value}>{children}</QueueProcessingContext.Provider>;
}

export function useQueueProcessing() {
  const context = useContext(QueueProcessingContext);
  if (!context) {
    throw new Error('useQueueProcessing must be used within QueueProcessingProvider');
  }
  return context;
}
