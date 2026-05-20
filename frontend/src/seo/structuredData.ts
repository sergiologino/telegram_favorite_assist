import { ServiceItem } from '../api/client';
import { absoluteUrl, SITE_DESCRIPTION, SITE_NAME } from './siteConfig';

export function buildWebsiteJsonLd() {
  return {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    name: SITE_NAME,
    description: SITE_DESCRIPTION,
    url: absoluteUrl('/'),
    inLanguage: 'ru-RU',
    potentialAction: {
      '@type': 'SearchAction',
      target: `${absoluteUrl('/')}?q={search_term_string}`,
      'query-input': 'required name=search_term_string',
    },
  };
}

export function buildWebPageJsonLd(title: string, description: string, path: string) {
  return {
    '@context': 'https://schema.org',
    '@type': 'WebPage',
    name: title,
    description,
    url: absoluteUrl(path),
    isPartOf: {
      '@type': 'WebSite',
      name: SITE_NAME,
      url: absoluteUrl('/'),
    },
    inLanguage: 'ru-RU',
  };
}

export function buildServiceJsonLd(service: ServiceItem) {
  return {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: service.title,
    description: service.description || undefined,
    url: absoluteUrl(`/service/${service.id}`),
    applicationCategory: service.category || 'UtilitiesApplication',
    image: service.imageUrl || undefined,
    sameAs: [service.appUrl, service.repoUrl].filter(Boolean),
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'RUB',
    },
  };
}

export function buildFindsFaqJsonLd() {
  return {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: [
      {
        '@type': 'Question',
        name: 'Что такое Finds?',
        acceptedAnswer: {
          '@type': 'Answer',
          text: 'Finds — подборка полезных и интересных сервисов, приложений и ресурсов от Altacod. Материалы собирались несколько лет из статей, заметок и разных источников и разложены по категориям и тегам.',
        },
      },
      {
        '@type': 'Question',
        name: 'Какие ресурсы можно найти в Finds?',
        acceptedAnswer: {
          '@type': 'Answer',
          text: 'В Finds — веб-сервисы, приложения, инструменты для разработки и продуктивности, open-source проекты на GitHub и другие полезные ссылки с кратким описанием.',
        },
      },
      {
        '@type': 'Question',
        name: 'Как искать в Finds?',
        acceptedAnswer: {
          '@type': 'Answer',
          text: 'Используйте поиск по названию и описанию, фильтр по категории, облако тегов и фильтр записей с GitHub-репозиторием.',
        },
      },
    ],
  };
}
