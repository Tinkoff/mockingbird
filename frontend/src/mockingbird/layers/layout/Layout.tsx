import type { ComponentType } from 'react';
import React from 'react';
import { Container } from '@platform-ui/navigation';
import { NotificationStack } from 'src/infrastructure/notifications';
import styles from './Layout.css';

interface Props {
  children: ComponentType<any>[];
}

export const Layout = (props: Props) => {
  const [header, page] = props.children;
  return (
    <div className={styles.root}>
      {header}
      <NotificationStack />
      <Container>
        <main className={styles.content}>{page}</main>
      </Container>
    </div>
  );
};
