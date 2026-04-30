import { formatClockTime, formatDateTimeIST } from './dateTime';

describe('dateTime utilities', () => {
  it('returns empty string for invalid date values', () => {
    expect(formatDateTimeIST('not-a-date')).toBe('');
  });

  it('formats valid dates with date and time separator', () => {
    const formatted = formatDateTimeIST('2026-04-16T10:30:00Z');
    expect(formatted).toContain('•');
    expect(formatted.length).toBeGreaterThan(5);
  });

  it('formats standard 24h clock values to 12h', () => {
    expect(formatClockTime('00:05')).toBe('12:05 AM');
    expect(formatClockTime('12:30')).toBe('12:30 PM');
    expect(formatClockTime('23:45')).toBe('11:45 PM');
  });

  it('clamps minutes and normalizes negative hours', () => {
    expect(formatClockTime('-1:99')).toBe('11:59 PM');
  });

  it('returns original value when clock value is not parseable', () => {
    expect(formatClockTime('not-time')).toBe('not-time');
  });

  it.skip('returns null for toDate with invalid input', () => {
    const { toDate } = jest.requireActual('./dateTime');
    expect(toDate('not-a-date')).toBeNull();
    expect(toDate({})).toBeNull();
  });

  it('returns empty string for formatDateTimeIST with invalid date', () => {
    expect(formatDateTimeIST('invalid-date')).toBe('');
    expect(formatDateTimeIST(NaN)).toBe('');
  });
});
