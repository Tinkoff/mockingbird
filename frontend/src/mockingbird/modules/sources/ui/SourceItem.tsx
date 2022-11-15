import React from 'react';
import { useNavigate } from '@tramvai/module-router';
import Island from '@platform-ui/island';
import { getPathSource } from 'src/mockingbird/paths';
import type { Source } from '../types';

interface Props {
  item: Source;
  serviceId: string;
}

export default function SourceItem({ item, serviceId }: Props) {
  const { name, description } = item;
  const onNavigate = useNavigate(getPathSource(serviceId, name));
  return (
    <Island title={name} text={description} onClick={onNavigate} clickable />
  );
}
