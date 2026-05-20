import { ServiceItem } from '../api/client';

export function parseTags(tags: string | null | undefined): string[] {
  if (!tags) {
    return [];
  }
  return tags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean);
}

export function extractAvailableTags(services: ServiceItem[]): string[] {
  const unique = new Set<string>();
  for (const service of services) {
    for (const tag of parseTags(service.tags)) {
      unique.add(tag);
    }
  }
  return [...unique].sort((left, right) => left.localeCompare(right, 'ru'));
}

export function matchesSelectedTags(service: ServiceItem, selectedTags: Set<string>): boolean {
  if (selectedTags.size === 0) {
    return true;
  }

  const serviceTags = new Set(parseTags(service.tags));
  for (const tag of selectedTags) {
    if (!serviceTags.has(tag)) {
      return false;
    }
  }
  return true;
}

export const GITHUB_CATEGORY_VALUE = '__github__';

export function parseCategoryFilter(value: string): { category?: string; hasRepo?: boolean } {
  if (value === GITHUB_CATEGORY_VALUE) {
    return { hasRepo: true };
  }
  if (!value) {
    return {};
  }
  return { category: value };
}

export function toCategoryFilterValue(category?: string, hasRepo?: boolean): string {
  if (hasRepo) {
    return GITHUB_CATEGORY_VALUE;
  }
  return category ?? '';
}
