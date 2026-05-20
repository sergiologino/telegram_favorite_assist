import { useEffect } from 'react';
import { absoluteUrl, DEFAULT_OG_IMAGE, SITE_BRAND, SITE_LOCALE } from './siteConfig';

export type PageSeoProps = {
  title: string;
  description: string;
  path?: string;
  image?: string | null;
  type?: 'website' | 'article';
  jsonLd?: Record<string, unknown> | Array<Record<string, unknown>>;
  noindex?: boolean;
};

function upsertMeta(name: string, content: string, attribute: 'name' | 'property' = 'name') {
  let element = document.head.querySelector(`meta[${attribute}="${name}"]`);
  if (!element) {
    element = document.createElement('meta');
    element.setAttribute(attribute, name);
    document.head.appendChild(element);
  }
  element.setAttribute('content', content);
}

function upsertLink(rel: string, href: string) {
  let element = document.head.querySelector(`link[rel="${rel}"]`);
  if (!element) {
    element = document.createElement('link');
    element.setAttribute('rel', rel);
    document.head.appendChild(element);
  }
  element.setAttribute('href', href);
}

function upsertJsonLd(data: PageSeoProps['jsonLd']) {
  const id = 'page-seo-jsonld';
  const existing = document.getElementById(id);
  if (existing) {
    existing.remove();
  }
  if (!data) {
    return;
  }

  const script = document.createElement('script');
  script.id = id;
  script.type = 'application/ld+json';
  script.textContent = JSON.stringify(data);
  document.head.appendChild(script);
}

export default function PageSeo({
  title,
  description,
  path = '/',
  image,
  type = 'website',
  jsonLd,
  noindex = false,
}: PageSeoProps) {
  useEffect(() => {
    const fullTitle = title.includes(SITE_BRAND) ? title : `${title} | ${SITE_BRAND}`;
    const canonical = absoluteUrl(path);
    const ogImage = image ? (image.startsWith('http') ? image : absoluteUrl(image)) : absoluteUrl(DEFAULT_OG_IMAGE);

    document.title = fullTitle;
    document.documentElement.lang = 'ru';

    upsertMeta('description', description);
    upsertMeta('robots', noindex ? 'noindex, nofollow' : 'index, follow');
    upsertMeta('googlebot', noindex ? 'noindex, nofollow' : 'index, follow');
    upsertLink('canonical', canonical);

    upsertMeta('og:title', fullTitle, 'property');
    upsertMeta('og:description', description, 'property');
    upsertMeta('og:type', type, 'property');
    upsertMeta('og:url', canonical, 'property');
    upsertMeta('og:site_name', SITE_BRAND, 'property');
    upsertMeta('og:locale', SITE_LOCALE, 'property');
    upsertMeta('og:image', ogImage, 'property');

    upsertMeta('twitter:card', 'summary_large_image');
    upsertMeta('twitter:title', fullTitle);
    upsertMeta('twitter:description', description);
    upsertMeta('twitter:image', ogImage);

    upsertJsonLd(jsonLd);
  }, [title, description, path, image, type, jsonLd, noindex]);

  return null;
}
