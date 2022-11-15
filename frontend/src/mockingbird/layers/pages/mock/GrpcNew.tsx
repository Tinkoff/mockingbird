import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import createStubStore from 'src/mockingbird/models/mockCreate/reducers/store';
import {
  createAction,
  resetCreateStateAction,
} from 'src/mockingbird/models/mockCreate/actions';
import FormGrpc from './FormGrpc';
import { mapFormDataToGrpc } from './utils';
import type { TGRPCFormData } from './types';

interface Props {
  labels: string[];
  serviceId: string;
}

export default function GrpcNew({ labels, serviceId }: Props) {
  const create = useActions(createAction);
  const { status } = useStoreSelector(createStubStore, selectorAsIs);
  const resetCreateState = useActions(resetCreateStateAction);
  useEffect(() => resetCreateState, [resetCreateState]);

  const onCreate = useCallback(
    (formData: TGRPCFormData) => {
      mapFormDataToGrpc(formData, serviceId).then((data) =>
        create({
          type: 'grpc',
          data,
          serviceId,
        })
      );
    },
    [serviceId, create]
  );

  return (
    <FormGrpc
      labels={labels}
      serviceId={serviceId}
      submitDisabled={status === 'loading'}
      onSubmit={onCreate}
    />
  );
}
