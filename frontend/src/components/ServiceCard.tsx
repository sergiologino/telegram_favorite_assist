import { Link } from 'react-router-dom';
import { ServiceItem } from '../api/client';
import { parseTags } from '../utils/catalogFilters';
import { getServiceLinks } from '../utils/serviceLinks';

type ServiceCardProps = {
  service: ServiceItem;
};

function formatDate(value: string | null) {
  if (!value) return '—';
  return new Date(value).toLocaleDateString('ru-RU');
}

export default function ServiceCard({ service }: ServiceCardProps) {
  const tags = parseTags(service.tags);
  const links = getServiceLinks(service);

  return (
    <article className="service-card">
      <Link to={`/service/${service.id}`} className="service-card-top">
        {service.imageUrl ? (
          <img className="service-card-image" src={service.imageUrl} alt={service.title} loading="lazy" />
        ) : (
          <div className="service-card-image" />
        )}
        <div className="service-card-body">
          <div className="meta-row">
            {service.category && <span className="badge badge-category">{service.category}</span>}
            <span className="muted">{formatDate(service.postedAt)}</span>
          </div>
          <h3>{service.title}</h3>
        </div>
      </Link>
      <div className="service-card-body service-card-footer">
        <p className="muted">{service.description?.slice(0, 140) || 'Без описания'}</p>
        {links.length > 0 && (
          <div className="links-row">
            {links.map((link) => (
              <a
                key={link.href}
                className="external-link"
                href={link.href}
                target="_blank"
                rel="noreferrer"
                onClick={(event) => event.stopPropagation()}
              >
                {link.label}
              </a>
            ))}
          </div>
        )}
        <div className="tag-row">
          {service.githubStars != null && <span className="badge badge-stars">★ {service.githubStars}</span>}
          {tags.slice(0, 3).map((tag) => (
            <span key={tag} className="badge badge-tag">
              {tag}
            </span>
          ))}
        </div>
      </div>
    </article>
  );
}
