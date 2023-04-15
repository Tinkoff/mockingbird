import React from 'react';
import type { ComponentType } from 'react';
import { MantineProvider, Container, SimpleGrid } from '@mantine/core';
import { NotificationStack } from 'src/infrastructure/notifications';

interface Props {
  children: ComponentType<any>[];
}

export const Layout = (props: Props) => {
  const [header, page] = props.children;
  return (
    <MantineProvider withGlobalStyles withNormalizeCSS>
      {header}
      <NotificationStack />
      <Container>
        <SimpleGrid cols={1}>{page}</SimpleGrid>
      </Container>
    </MantineProvider>
  );
};
