import { describe, expect, it } from 'vitest';
import { getServiceLinks } from './serviceLinks';
import { ServiceItem } from '../api/client';

function service(overrides: Partial<ServiceItem>): ServiceItem {
  return {
    id: 1,
    title: 'Test',
    description: null,
    imageUrl: null,
    appUrl: null,
    repoUrl: null,
    githubStars: null,
    category: null,
    categorySlug: null,
    tags: null,
    postedAt: null,
    createdAt: '2024-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('getServiceLinks', () => {
  it('returns repo and site links', () => {
    const links = getServiceLinks(
      service({
        appUrl: 'https://www.drawdb.app/',
        repoUrl: 'https://github.com/drawdb-io/drawdb',
      }),
    );
    expect(links).toHaveLength(2);
    expect(links[0].label).toBe('Перейти к сервису');
    expect(links[1].label).toBe('Открыть репозиторий');
  });

  it('deduplicates identical urls', () => {
    const links = getServiceLinks(
      service({
        appUrl: 'https://github.com/foo/bar',
        repoUrl: 'https://github.com/foo/bar',
      }),
    );
    expect(links).toHaveLength(1);
  });
});
