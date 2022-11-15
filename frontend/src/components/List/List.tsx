import React from 'react';
import styles from './List.css';

interface Props {
  children: React.ReactNode;
}

export default function List({ children }: Props) {
  return <div className={styles.root}>{children}</div>;
}
