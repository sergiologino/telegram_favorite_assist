import { FormEvent, useEffect, useMemo, useState } from 'react';
import { api, Category, ServiceItem } from '../api/client';
import ServiceCard from '../components/ServiceCard';
import TagCloud from '../components/TagCloud';
import { useQueueProcessing } from '../hooks/useQueueProcessing';
import PageSeo from '../seo/PageSeo';
import { SITE_DESCRIPTION, SITE_NAME } from '../seo/siteConfig';
import { buildFindsFaqJsonLd, buildWebPageJsonLd, buildWebsiteJsonLd } from '../seo/structuredData';
import {
  extractAvailableTags,
  GITHUB_CATEGORY_VALUE,
  matchesSelectedTags,
  parseCategoryFilter,
} from '../utils/catalogFilters';

export default function CatalogPage() {
  const [baseServices, setBaseServices] = useState<ServiceItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [query, setQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [selectedTags, setSelectedTags] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { stats, batchVersion } = useQueueProcessing();

  const availableTags = useMemo(() => extractAvailableTags(baseServices), [baseServices]);
  const displayedServices = useMemo(
    () => baseServices.filter((service) => matchesSelectedTags(service, selectedTags)),
    [baseServices, selectedTags],
  );

  async function loadPrimaryFilters(
    params?: {
      q?: string;
      categoryFilter?: string;
      from?: string;
      to?: string;
    },
    options?: { silent?: boolean; keepTags?: boolean },
  ) {
    const nextQuery = params?.q ?? query;
    const nextCategoryFilter = params?.categoryFilter ?? categoryFilter;
    const nextFrom = params?.from ?? from;
    const nextTo = params?.to ?? to;

    if (!options?.silent) {
      setLoading(true);
    }
    setError(null);
    if (!options?.keepTags) {
      setSelectedTags(new Set());
    }

    setQuery(nextQuery);
    setCategoryFilter(nextCategoryFilter);
    setFrom(nextFrom);
    setTo(nextTo);

    const { category, hasRepo } = parseCategoryFilter(nextCategoryFilter);

    try {
      const [page, categoryList] = await Promise.all([
        api.getServices({ q: nextQuery, category, from: nextFrom, to: nextTo, hasRepo, size: 1000 }),
        api.getCategories(),
      ]);
      setBaseServices(page.items);
      setCategories(categoryList);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Ошибка загрузки');
    } finally {
      if (!options?.silent) {
        setLoading(false);
      }
    }
  }

  useEffect(() => {
    loadPrimaryFilters({ q: '', categoryFilter: '', from: '', to: '' });
  }, []);

  useEffect(() => {
    if (batchVersion === 0) {
      return;
    }
    loadPrimaryFilters(undefined, { silent: true, keepTags: true });
  }, [batchVersion]);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    loadPrimaryFilters();
  }

  function handleCategoryChange(value: string) {
    loadPrimaryFilters({ categoryFilter: value });
  }

  function handleResetFilters() {
    loadPrimaryFilters({ q: '', categoryFilter: '', from: '', to: '' });
  }

  function toggleTag(tag: string) {
    setSelectedTags((current) => {
      const next = new Set(current);
      if (next.has(tag)) {
        next.delete(tag);
      } else {
        next.add(tag);
      }
      return next;
    });
  }

  const pendingCount = stats?.pendingPosts ?? 0;
  const githubCount = stats?.githubServices ?? 0;
  const hasActiveFilters =
    query !== '' || categoryFilter !== '' || from !== '' || to !== '' || selectedTags.size > 0;

  return (
    <>
      <PageSeo
        title="Полезные сервисы и приложения"
        description={SITE_DESCRIPTION}
        path="/"
        jsonLd={[buildWebsiteJsonLd(), buildWebPageJsonLd(SITE_NAME, SITE_DESCRIPTION, '/'), buildFindsFaqJsonLd()]}
      />

      <header className="page-header">
        <h1>Finds — полезные сервисы и приложения</h1>
        <p className="page-lead">
          Интересные ресурсы, которые встретились за последние 4 года: сервисы, инструменты и приложения из статей,
          заметок и разных источников — с описанием, категорией, тегами и ссылками на сайт или GitHub.
        </p>
      </header>

      {stats && (
        <section className="stats-grid" aria-labelledby="stats-heading">
          <h2 id="stats-heading" className="visually-hidden">
            Статистика каталога
          </h2>
          <div className="stat-card">
            <strong>{stats.totalServices}</strong>
            <span>Сервисов</span>
          </div>
          <div className="stat-card">
            <strong>{stats.totalCategories}</strong>
            <span>Категорий</span>
          </div>
          <div className="stat-card">
            <strong>{stats.pendingPosts}</strong>
            <span>В очереди</span>
          </div>
          <div className="stat-card">
            <strong>{stats.failedPosts}</strong>
            <span>Ошибок</span>
          </div>
        </section>
      )}

      <section aria-labelledby="filters-heading">
        <h2 id="filters-heading" className="visually-hidden">
          Поиск и фильтры
        </h2>
        <form className="filters" onSubmit={handleSubmit}>
        <input
          placeholder="Поиск по названию, описанию, ссылкам..."
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <select value={categoryFilter} onChange={(event) => handleCategoryChange(event.target.value)}>
          <option value="">Все категории</option>
          <option value={GITHUB_CATEGORY_VALUE}>GitHub ({githubCount})</option>
          {categories.map((item) => (
            <option key={item.slug} value={item.slug}>
              {item.name} ({item.count})
            </option>
          ))}
        </select>
        <input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
        <input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
        <button className="primary" type="submit">
          Найти
        </button>
        <button
          className="secondary"
          type="button"
          onClick={handleResetFilters}
          disabled={loading || !hasActiveFilters}
        >
          Сбросить
        </button>
        </form>
      </section>

      <TagCloud tags={availableTags} selectedTags={selectedTags} onToggle={toggleTag} />

      {!loading && <hr className="catalog-divider" />}

      <section aria-labelledby="catalog-heading">
        <h2 id="catalog-heading" className="visually-hidden">
          Список сервисов
        </h2>
      {loading && <div className="empty-state">Загрузка каталога...</div>}
      {error && <div className="error-state">{error}</div>}
      {!loading && !error && displayedServices.length === 0 && pendingCount > 0 && baseServices.length === 0 && (
        <div className="empty-state">
          Сервисы ещё обрабатываются — карточки появятся по мере готовности.
        </div>
      )}
      {!loading && !error && displayedServices.length === 0 && baseServices.length > 0 && (
        <div className="empty-state">Нет карточек по выбранным тегам. Снимите часть тегов или измените фильтры.</div>
      )}
      {!loading && !error && displayedServices.length === 0 && baseServices.length === 0 && pendingCount === 0 && (
        <div className="empty-state">Каталог пока пуст — скоро здесь появятся находки.</div>
      )}

      <section className="cards-grid">
        {displayedServices.map((service) => (
          <ServiceCard key={service.id} service={service} />
        ))}
      </section>
      </section>
    </>
  );
}
