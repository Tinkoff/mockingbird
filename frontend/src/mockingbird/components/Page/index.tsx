import React from 'react';
import styles from './Page.css';

interface Props {
  children: React.ReactNode;
}

export default function Page({ children }: Props) {
  return <div className={styles.root}>{children}</div>;
}
