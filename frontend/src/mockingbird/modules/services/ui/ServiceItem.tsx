import React, { useCallback } from 'react';
import { useNavigate } from '@tramvai/module-router';
import Island from '@platform-ui/island';
import Link from '@platform-ui/link';
import Text from '@platform-ui/text';
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
    <Island clickable onClick={handleClick}>
      <div className={styles.root}>
        <div className={styles.block}>
          <Text size={17}>
            <Link onClick={handleClick}>{name}</Link>
          </Text>
          <Text size={13}>{`${execApiPath}/${suffix}`}</Text>
        </div>
        <div className={styles.blockLinks}>
          <Link onClick={navigateSources}>Источники</Link>
          <Link onClick={navigateDestinations}>Получатели</Link>
        </div>
      </div>
    </Island>
  );
}

function isClick(event: MouseEvent) {
  let found = event.target;
  const root = event.currentTarget;
  while (found && found !== root) {
    if (found.tagName === 'A' || found.tagName === 'BUTTON') return;
    found = found.parentElement;
  }
  const selection = window.getSelection()?.toString();
  return !selection || selection.length === 0;
}
