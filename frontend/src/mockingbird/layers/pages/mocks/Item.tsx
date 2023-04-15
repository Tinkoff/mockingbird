import React from 'react';
import { Paper, Text, Chip, Anchor } from '@mantine/core';
import pluralize from 'src/mockingbird/infrastructure/pluralize';
import styles from './Item.css';

type Props = {
  name: string;
  description: string;
  scope: string;
  times?: number;
  labels: string[];
  onClick: () => void;
};

export default function Item(props: Props) {
  // [удалить] хак с [] - лейблов для сценариев пока нет
  const { onClick, name, description, scope, times, labels = [] } = props;
  return (
    <Paper onClick={onClick} shadow="xs">
      <div className={styles.root}>
        <div className={styles.block}>
          <Anchor size="md">{name}</Anchor>
          <Text size="sm">{description}</Text>
        </div>
        <div className={styles.block}>
          <Text size="md">{getScopeText(scope, times)}</Text>
          <div className={styles.tags}>
            {labels.map((label) => (
              <Chip size="xs" key={label} color={getBadgeColor(label)}>
                {label}
              </Chip>
            ))}
          </div>
        </div>
      </div>
    </Paper>
  );
}

function getScopeText(scope: string, times?: number) {
  if (scope === 'persistent') return 'Вечный';
  if (scope === 'ephemeral') return 'Эфемерный';
  if (scope === 'countdown' && typeof times === 'number') {
    return `Осталось ${pluralize(times, 'вызов', 'вызова', 'вызовов')}`;
  }
  return '';
}

type DefaultMantineColor =
  | 'dark'
  | 'gray'
  | 'red'
  | 'pink'
  | 'grape'
  | 'violet'
  | 'indigo'
  | 'blue'
  | 'cyan'
  | 'green'
  | 'lime'
  | 'yellow'
  | 'orange'
  | 'teal';

const BADGE_COLORS: DefaultMantineColor[] = [
  'violet',
  'blue',
  'gray',
  'green',
  'yellow',
  'teal',
  'red',
  'cyan',
  'orange',
];

function getBadgeColor(text: string) {
  if (!text) return BADGE_COLORS[0];
  let sum = 0;
  for (let i = 0, l = text.length; i < l; i++) {
    sum += text.charCodeAt(i);
  }
  return BADGE_COLORS[sum % BADGE_COLORS.length];
}
