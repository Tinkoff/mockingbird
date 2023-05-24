import React, { useCallback } from 'react';
import { useNavigate } from '@tramvai/module-router';
import { Paper, Anchor, Text } from '@mantine/core';
import {
  getPathMocks,
  getPathSources,
  getPathDestinations,
} from 'src/mockingbird/paths';
import type { Service } from '../types';
import styles from './ServiceItem.css';

interface Props {
  execApiPath: string;
  item: Service;
}

export default function ServiceItem({ execApiPath, item }: Props) {
  const { name, suffix } = item;
  const navigate = useNavigate(getPathMocks(suffix));
  const navigateSources = useNavigate(getPathSources(suffix));
  const navigateDestinations = useNavigate(getPathDestinations(suffix));
  const handleClick = useCallback(
    (event) => {
      if (isClick(event)) navigate();
    },
    [navigate]
  );
  return (
    <Paper onClick={handleClick} shadow="xs">
      <div className={styles.root}>
        <div className={styles.block}>
          <Anchor
            size="md"
            component="button"
            type="button"
            onClick={handleClick}
          >
            {name}
          </Anchor>
          <Text size="xs">{`${execApiPath}/${suffix}`}</Text>
        </div>
        <div className={styles.blockLinks}>
          <Anchor
            size="sm"
            component="button"
            type="button"
            onClick={navigateSources}
          >
            Источники
          </Anchor>
          <Anchor
            size="sm"
            component="button"
            type="button"
            onClick={navigateDestinations}
          >
            Получатели
          </Anchor>
        </div>
      </div>
    </Paper>
  );
}

function isClick(event: MouseEvent) {
  let found = event.target;
  const root = event.currentTarget;
  while (found && found !== root) {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    if (found.tagName === 'A' || found.tagName === 'BUTTON') return;
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    found = found.parentElement;
  }
  const selection = window.getSelection()?.toString();
  return !selection || selection.length === 0;
}
