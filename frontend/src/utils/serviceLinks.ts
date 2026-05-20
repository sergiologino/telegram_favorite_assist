import { ServiceItem } from '../api/client';

export type ServiceLink = {
  href: string;
  label: string;
};

export function getServiceLinks(service: ServiceItem): ServiceLink[] {
  const links: ServiceLink[] = [];
  const seen = new Set<string>();

  if (service.appUrl && !seen.has(service.appUrl)) {
    links.push({ href: service.appUrl, label: 'Перейти к сервису' });
    seen.add(service.appUrl);
  }

  if (service.repoUrl && !seen.has(service.repoUrl)) {
    links.push({ href: service.repoUrl, label: 'Открыть репозиторий' });
  }

  return links;
}
