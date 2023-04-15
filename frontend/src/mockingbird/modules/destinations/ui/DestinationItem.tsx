import React from 'react';
import { useNavigate } from '@tramvai/module-router';
import { Box, Paper, Text } from '@mantine/core';
import { getPathDestination } from 'src/mockingbird/paths';
import type { Destination } from '../types';

type Props = {
  item: Destination;
  serviceId: string;
};

export default function DestinationItem({ item, serviceId }: Props) {
  const { name, description } = item;
  const onNavigate = useNavigate(getPathDestination(serviceId, name));
  return (
    <Paper onClick={onNavigate} shadow="xs" p="md">
      <Box w="100%">
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
