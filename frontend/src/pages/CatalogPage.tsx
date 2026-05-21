import { FormEvent, useEffect, useState } from 'react';
import { api, Category, ServiceItem } from '../api/client';
import Pagination from '../components/Pagination';
import ServiceCard from '../components/ServiceCard';
import TagCloud from '../components/TagCloud';
import { useQueueProcessing } from '../hooks/useQueueProcessing';
import PageSeo from '../seo/PageSeo';
import { SITE_DESCRIPTION, SITE_NAME } from '../seo/siteConfig';
import { buildFindsFaqJsonLd, buildWebPageJsonLd, buildWebsiteJsonLd } from '../seo/structuredData';
import { GITHUB_CATEGORY_VALUE, parseCategoryFilter } from '../utils/catalogFilters';

const PAGE_SIZE = 20;

export default function CatalogPage() {
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [availableTags, setAvailableTags] = useState<string[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [query, setQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [selectedTags, setSelectedTags] = useState<Set<string>>(new Set());
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { stats, batchVersion } = useQueueProcessing();

  async function loadCatalog(
    nextPage = page,
    params?: {
      q?: string;
      categoryFilter?: string;
      from?: string;
      to?: string;
      tags?: Set<string>;
    },
    options?: { silent?: boolean; scrollToTop?: boolean },
  ) {
    const nextQuery = params?.q ?? query;
    const nextCategoryFilter = params?.categoryFilter ?? categoryFilter;
    const nextFrom = params?.from ?? from;
    const nextTo = params?.to ?? to;
    const nextTags = params?.tags ?? selectedTags;

    if (!options?.silent) {
      setLoading(true);
    }
    setError(null);

    setQuery(nextQuery);
    setCategoryFilter(nextCategoryFilter);
    setFrom(nextFrom);
    setTo(nextTo);
    if (params?.tags !== undefined) {
      setSelectedTags(new Set(nextTags));
    }
    setPage(nextPage);

    const { category, hasRepo } = parseCategoryFilter(nextCategoryFilter);
    const catalogFilters = { q: nextQuery, category, from: nextFrom, to: nextTo, hasRepo };
    const tagsParam = nextTags.size > 0 ? [...nextTags].join(',') : undefined;

    try {
      const [servicesPage, categoryList, tags] = await Promise.all([
        api.getServices({ ...catalogFilters, tags: tagsParam, page: nextPage, size: PAGE_SIZE }),
        api.getCategories(),
        api.getTags(catalogFilters),
      ]);

      setServices(servicesPage.items);
      setTotalPages(servicesPage.totalPages);
      setTotalElements(servicesPage.totalElements);
      setCategories(categoryList);
      setAvailableTags(tags);

      if (options?.scrollToTop) {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Ошибка загрузки');
    } finally {
      if (!options?.silent) {
        setLoading(false);
      }
    }
  }

  useEffect(() => {
    loadCatalog(0, { q: '', categoryFilter: '', from: '', to: '', tags: new Set() });
  }, []);

  useEffect(() => {
    if (batchVersion === 0) {
      return;
    }
    loadCatalog(page, undefined, { silent: true });
  }, [batchVersion]);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    loadCatalog(0);
  }

  function handleCategoryChange(value: string) {
    loadCatalog(0, { categoryFilter: value });
  }

  function handleResetFilters() {
    loadCatalog(0, { q: '', categoryFilter: '', from: '', to: '', tags: new Set() });
  }

  function toggleTag(tag: string) {
    const nextTags = new Set(selectedTags);
    if (nextTags.has(tag)) {
      nextTags.delete(tag);
    } else {
      nextTags.add(tag);
    }
    loadCatalog(0, { tags: nextTags });
  }

  function handlePageChange(nextPage: number) {
    loadCatalog(nextPage, undefined, { scrollToTop: true });
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
        {!loading && !error && totalElements === 0 && pendingCount > 0 && !hasActiveFilters && (
          <div className="empty-state">Сервисы ещё обрабатываются — карточки появятся по мере готовности.</div>
        )}
        {!loading && !error && totalElements === 0 && hasActiveFilters && (
          <div className="empty-state">Ничего не найдено. Измените фильтры или снимите часть тегов.</div>
        )}
        {!loading && !error && totalElements === 0 && !hasActiveFilters && pendingCount === 0 && (
          <div className="empty-state">Каталог пока пуст — скоро здесь появятся находки.</div>
        )}

        <section className="cards-grid">
          {services.map((service) => (
            <ServiceCard key={service.id} service={service} />
          ))}
        </section>

        {!loading && !error && (
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={PAGE_SIZE}
            onPageChange={handlePageChange}
            disabled={loading}
          />
        )}
      </section>
    </>
  );
}
