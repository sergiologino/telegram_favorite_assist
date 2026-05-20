export const SITE_BRAND = 'Finds';

export const SITE_NAME = 'Finds — полезные сервисы и приложения';

export const SITE_DESCRIPTION =
  'Подборка Altacod: полезные и интересные сервисы, приложения и ресурсы, собранные за последние 4 года из статей, заметок и разных источников. Поиск, категории, теги и GitHub.';

export const SITE_LOCALE = 'ru_RU';

export const SITE_HOME_URL = 'https://finds.altacod.com';

export function getSiteUrl(): string {
  const configured = import.meta.env.VITE_SITE_URL?.trim();
  if (configured) {
    return configured.replace(/\/+$/, '');
  }
  if (typeof window !== 'undefined') {
    return window.location.origin;
  }
  return SITE_HOME_URL;
}

export function absoluteUrl(path = '/'): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${getSiteUrl()}${normalizedPath}`;
}

export const DEFAULT_OG_IMAGE = '/og-default.svg';
