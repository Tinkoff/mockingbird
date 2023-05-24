import React from 'react';
import styles from './Shadow.css';

type Props = { children: React.ReactNode };

export function Shadow(props: Props) {
  const { children } = props;
  return <div className={styles.root}>{children}</div>;
}
