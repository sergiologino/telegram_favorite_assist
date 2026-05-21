const METRIKA_SCRIPT = 'https://mc.yandex.ru/metrika/tag.js';

export function getYandexMetrikaId(): number | null {
  const raw = import.meta.env.VITE_YANDEX_METRIKA_ID?.trim();
  if (!raw) {
    return null;
  }

  const id = Number(raw);
  return Number.isFinite(id) && id > 0 ? id : null;
}

export function loadYandexMetrika(id: number): void {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return;
  }

  for (let index = 0; index < document.scripts.length; index += 1) {
    if (document.scripts[index].src.includes(METRIKA_SCRIPT)) {
      return;
    }
  }

  const stub = window.ym;
  if (typeof stub !== 'function') {
    const queue: unknown[][] = [];
    const ymFn = (...args: unknown[]) => {
      queue.push(args);
    };
    ymFn.a = queue;
    window.ym = ymFn as YandexMetrikaFn;
  }

  const script = document.createElement('script');
  script.async = true;
  script.src = `${METRIKA_SCRIPT}?id=${id}`;
  document.head.appendChild(script);
}

export function initYandexMetrika(id: number): void {
  if (typeof window.ym !== 'function') {
    return;
  }

  window.ym(id, 'init', {
    ssr: true,
    webvisor: true,
    clickmap: true,
    ecommerce: 'dataLayer',
    referrer: document.referrer,
    url: location.href,
    accurateTrackBounce: true,
    trackLinks: true,
  });
}

export function trackYandexMetrikaHit(id: number, url: string): void {
  if (typeof window.ym !== 'function') {
    return;
  }

  window.ym(id, 'hit', url, {
    referer: document.referrer,
  });
}

export type YandexMetrikaFn = ((counterId: number, event: string, ...params: unknown[]) => void) & {
  a?: unknown[][];
  l?: number;
};

declare global {
  interface Window {
    ym?: YandexMetrikaFn;
  }
}
