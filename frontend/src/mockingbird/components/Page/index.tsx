import type { ReactNode } from 'react';
import styles from './Page.css';

type Props = {
  children: ReactNode;
};

export default function Page({ children }: Props) {
  return <div className={styles.root}>{children}</div>;
}
