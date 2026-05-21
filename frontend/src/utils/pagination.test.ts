import { describe, expect, it } from 'vitest';
import { buildPageNumbers, formatPageRange } from './pagination';

describe('pagination', () => {
  it('builds compact page list for many pages', () => {
    expect(buildPageNumbers(5, 20)).toEqual([0, 4, 5, 6, 19]);
  });

  it('formats visible range', () => {
    expect(formatPageRange(1, 20, 915)).toBe('Показано 21–40 из 915');
  });
});
