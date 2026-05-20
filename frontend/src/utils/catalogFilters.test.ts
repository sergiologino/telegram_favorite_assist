import { describe, expect, it } from 'vitest';
import { extractAvailableTags, matchesSelectedTags, parseCategoryFilter, parseTags } from './catalogFilters';
import { ServiceItem } from '../api/client';

function service(tags: string | null): ServiceItem {
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
    tags,
    postedAt: null,
    createdAt: '2024-01-01T00:00:00Z',
  };
}

describe('catalogFilters', () => {
  it('parses tags from comma-separated string', () => {
    expect(parseTags('ai, voice, tts')).toEqual(['ai', 'voice', 'tts']);
  });

  it('filters services by selected tags with AND logic', () => {
    const items = [service('ai, voice'), service('ai, video'), service('video')];
    const selected = new Set(['ai', 'voice']);
    expect(items.filter((item) => matchesSelectedTags(item, selected))).toEqual([service('ai, voice')]);
  });

  it('builds tag cloud from filtered services', () => {
    const tags = extractAvailableTags([service('voice, ai'), service('video, ai')]);
    expect(tags).toEqual(['ai', 'video', 'voice']);
  });

  it('maps github pseudo-category', () => {
    expect(parseCategoryFilter('__github__')).toEqual({ hasRepo: true });
    expect(parseCategoryFilter('tools')).toEqual({ category: 'tools' });
  });
});
