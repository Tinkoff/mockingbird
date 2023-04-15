import React from 'react';
import { useNavigate } from '@tramvai/module-router';
import { Paper, Text, Box } from '@mantine/core';
import { getPathSource } from 'src/mockingbird/paths';
import type { Source } from '../types';

type Props = {
  item: Source;
  serviceId: string;
};

export default function SourceItem({ item, serviceId }: Props) {
  const { name, description } = item;
  const onNavigate = useNavigate(getPathSource(serviceId, name));
  return (
    <Paper onClick={onNavigate} shadow="xs" p="md">
      <Box>
        <Text size="md">{name}</Text>
      </Box>
      <Box>
        <Text size="sm" color="gray">
          {description}
        </Text>
      </Box>
    </Paper>
  );
}
