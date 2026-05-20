import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, ServiceItem } from '../api/client';
import { parseTags } from '../utils/catalogFilters';
import { getServiceLinks } from '../utils/serviceLinks';
import PageSeo from '../seo/PageSeo';
import { buildServiceJsonLd, buildWebPageJsonLd } from '../seo/structuredData';

export default function ServiceDetailPage() {
  const { id } = useParams();
  const [service, setService] = useState<ServiceItem | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    api.getService(Number(id))
      .then(setService)
      .catch((loadError) => setError(loadError instanceof Error ? loadError.message : 'Ошибка'));
  }, [id]);

  const seoDescription = useMemo(() => {
    if (!service) {
      return 'Карточка ресурса из Finds — подборки полезных сервисов и приложений Altacod.';
    }
    return (service.description || service.title).slice(0, 160);
  }, [service]);

  if (error) {
    return <div className="error-state">{error}</div>;
  }

  if (!service) {
    return (
      <>
        <PageSeo title="Загрузка сервиса" description="Загрузка карточки сервиса в Finds." path={`/service/${id}`} noindex />
        <div className="empty-state">Загрузка карточки...</div>
      </>
    );
  }

  const tags = parseTags(service.tags);
  const links = getServiceLinks(service);

  return (
    <>
      <PageSeo
        title={service.title}
        description={seoDescription}
        path={`/service/${service.id}`}
        image={service.imageUrl}
        type="article"
        jsonLd={[
          buildWebPageJsonLd(service.title, seoDescription, `/service/${service.id}`),
          buildServiceJsonLd(service),
        ]}
      />

      <article className="detail-card">
        <Link to="/" className="muted">
          ← Назад к каталогу
        </Link>
        {service.imageUrl && <img className="detail-image" src={service.imageUrl} alt={service.title} />}
        <div className="meta-row">
          {service.category && <span className="badge badge-category">{service.category}</span>}
          {service.githubStars != null && <span className="badge badge-stars">★ {service.githubStars}</span>}
        </div>
        <h1>{service.title}</h1>
        <h2 className="visually-hidden">Описание и ссылки</h2>
        <p>{service.description || 'Без описания'}</p>
        {links.length > 0 && (
          <div className="links-row">
            {links.map((link) => (
              <a key={link.href} className="external-link" href={link.href} target="_blank" rel="noreferrer">
                {link.label}
              </a>
            ))}
          </div>
        )}
        {tags.length > 0 && (
          <section aria-labelledby="service-tags-heading">
            <h2 id="service-tags-heading" className="section-subtitle">
              Теги
            </h2>
            <div className="tag-row">
              {tags.map((tag) => (
                <span key={tag} className="badge badge-tag">
                  {tag}
                </span>
              ))}
            </div>
          </section>
        )}
      </article>
    </>
  );
}
