/** Получить склонение слова */
export function plural(
  count: number,
  singular: string,
  plural1: string,
  plural2: string = plural1
) {
  const hasFloatingPoint = count % 1 !== 0;
  if (hasFloatingPoint) return plural2;
  const c1 = Math.abs(count % 100);
  if (c1 >= 5 && c1 <= 20) return plural2;
  const c2 = Math.abs(c1 % 10);
  if (c2 === 1) return singular;
  if (c2 >= 2 && c2 <= 4) return plural1;
  return plural2;
}

/** Получить отформатированное число со склоняемым словом */
export default function pluralize(
  count: number,
  singular: string,
  plural1: string,
  plural2: string = plural1
) {
  return `${count} ${plural(count, singular, plural1, plural2)}`;
}
