import React from 'react';
import type { FlexProps } from '@mantine/core';
import { Flex, Loader } from '@mantine/core';

export function ListLoading(props: FlexProps) {
  return (
    <Flex
      mih={80}
      gap="md"
      justify="center"
      align="center"
      direction="row"
      {...props}
    >
      <Loader size="lg" />
    </Flex>
  );
}
