const toDate = (value: string | number | Date): Date | null => {
  const dateValue = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(dateValue.getTime())) {
    return null;
  }
  return dateValue;
};

export const formatDateTimeIST = (value: string | number | Date): string => {
  const dateValue = toDate(value);
  if (!dateValue) return '';

  const locale = typeof navigator !== 'undefined' && navigator.language ? navigator.language : 'en-US';

  const datePart = dateValue.toLocaleDateString(locale, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });

  const timePart = dateValue.toLocaleTimeString(locale, {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });

  return `${datePart} • ${timePart}`;
};

export const formatClockTime = (timeValue: string): string => {
  const [hoursRaw, minutesRaw] = String(timeValue).split(':');
  const hours = Number(hoursRaw);
  const minutes = Number(minutesRaw ?? 0);

  if (!Number.isFinite(hours) || !Number.isFinite(minutes)) {
    return String(timeValue);
  }

  const normalizedHours = ((hours % 24) + 24) % 24;
  const suffix = normalizedHours >= 12 ? 'PM' : 'AM';
  const displayHours = normalizedHours % 12 || 12;
  const displayMinutes = String(Math.max(0, Math.min(59, minutes))).padStart(2, '0');

  return `${displayHours}:${displayMinutes} ${suffix}`;
};
