import React from 'react';
import { useNavigate } from '@tramvai/module-router';
import type { TGRPCMock } from 'src/mockingbird/models/mock/types';
import { getPathMock } from 'src/mockingbird/paths';
import Item from './Item';

interface Props {
  serviceId: string;
  item: TGRPCMock;
}

export default function GRPCItem({ serviceId, item }: Props) {
  const { id, name, methodName, scope, times, labels } = item;
  const navigate = useNavigate(getPathMock(serviceId, id, 'grpc'));
  return (
    <Item
      name={name}
      description={methodName}
      scope={scope}
      times={times}
      labels={labels}
      onClick={navigate}
    />
  );
}
