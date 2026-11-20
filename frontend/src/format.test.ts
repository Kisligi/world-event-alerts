import { describe, expect, it } from 'vitest';
import { formatDateTime, parseKeywords } from './format';

describe('parseKeywords', () => {
  it('splits on commas, trims, and drops empty entries', () => {
    expect(parseKeywords(' earthquake, Japan ,, tsunami ,')).toEqual(['earthquake', 'Japan', 'tsunami']);
  });

  it('drops repeats regardless of case, keeping the first spelling', () => {
    expect(parseKeywords('Japan, japan, JAPAN, Tokyo')).toEqual(['Japan', 'Tokyo']);
  });

  it('returns nothing for blank input', () => {
    expect(parseKeywords('   ')).toEqual([]);
  });
});

describe('formatDateTime', () => {
  it('shows day, month, year and time (tests run in UTC)', () => {
    expect(formatDateTime('2026-11-20T13:28:45Z')).toBe('20 Nov 2026, 13:28');
  });
});
