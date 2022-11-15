import React from 'react';
import { useNavigate } from '@tramvai/module-router';
import Island from '@platform-ui/island';
import { getPathDestination } from 'src/mockingbird/paths';
import type { Destination } from '../types';

interface Props {
  item: Destination;
  serviceId: string;
}

export default function DestinationItem({ item, serviceId }: Props) {
  const { name, description } = item;
  const onNavigate = useNavigate(getPathDestination(serviceId, name));
  return (
    <Island title={name} text={description} onClick={onNavigate} clickable />
  );
}
