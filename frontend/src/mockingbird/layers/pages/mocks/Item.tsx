import React from 'react';
import type { BadgeProps } from '@platform-ui/badge';
import Badge from '@platform-ui/badge';
import Island from '@platform-ui/island';
import Text from '@platform-ui/text';
import pluralize from 'src/mockingbird/infrastructure/pluralize';
import styles from './Item.css';

interface Props {
  name: string;
  description: string;
  scope: string;
  times: number;
  labels: string[];
  onClick: () => void;
}

export default function Item(props: Props) {
  // [удалить] хак с [] - лейблов для сценариев пока нет
  const { onClick, name, description, scope, times, labels = [] } = props;
  return (
    <Island clickable onClick={onClick}>
      <div className={styles.root}>
        <div className={styles.block}>
          <Text size={17}>{name}</Text>
          <Text size={13}>{description}</Text>
        </div>
        <div className={styles.block}>
          <Text size={17}>{getScopeText(scope, times)}</Text>
          <div className={styles.tags}>
            {labels.map((label) => (
              <Badge size="m" key={label} color={getBadgeColor(label)}>
                {label}
              </Badge>
            ))}
          </div>
        </div>
      </div>
    </Island>
  );
}

function getScopeText(scope: string, times: number) {
  if (scope === 'persistent') return 'Вечный';
  if (scope === 'ephemeral') return 'Эфемерный';
  if (scope === 'countdown') {
    return `Осталось ${pluralize(times, 'вызов', 'вызова', 'вызовов')}`;
  }
  return '';
}

const BADGE_COLORS: BadgeProps['color'][] = [
  'onLight',
  'blue',
  'gray',
  'green',
  'yellow',
  'whiteBlock',
  'red',
  'lightBlue',
  'highlight',
];

function getBadgeColor(text: string) {
  if (!text) return BADGE_COLORS[0];
  let sum = 0;
  for (let i = 0, l = text.length; i < l; i++) {
    sum += text.charCodeAt(i);
  }
  return BADGE_COLORS[sum % BADGE_COLORS.length];
}
