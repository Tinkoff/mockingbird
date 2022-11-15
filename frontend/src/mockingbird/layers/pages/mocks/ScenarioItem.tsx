import React from 'react';
import { useNavigate } from '@tramvai/module-router';
import type { TScenarioMock } from 'src/mockingbird/models/mock/types';
import { getPathMock } from 'src/mockingbird/paths';
import Item from './Item';

interface Props {
  serviceId: string;
  item: TScenarioMock;
}

export default function ScenarioItem({ serviceId, item }: Props) {
  const { id, name, source, destination = '', scope, times, labels } = item;
  const navigate = useNavigate(getPathMock(serviceId, id, 'scenario'));
  return (
    <Item
      name={name}
      description={`${source}${destination ? ` -> ${destination}` : ''}`}
      scope={scope}
      times={times}
      labels={labels}
      onClick={navigate}
    />
  );
}
