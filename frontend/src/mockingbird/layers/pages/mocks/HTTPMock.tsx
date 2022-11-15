import React from 'react';
import { useNavigate } from '@tramvai/module-router';
import type { THTTPMock } from 'src/mockingbird/models/mock/types';
import { getPathMock } from 'src/mockingbird/paths';
import Item from './Item';

interface Props {
  serviceId: string;
  item: THTTPMock;
}

export default function HTTPMock({ serviceId, item }: Props) {
  const { id, name, method, path, pathPattern, scope, times, labels } = item;
  const navigate = useNavigate(getPathMock(serviceId, id, 'http'));
  return (
    <Item
      name={name}
      description={`${method} ${path || pathPattern}`}
      scope={scope}
      times={times}
      labels={labels}
      onClick={navigate}
    />
  );
}
