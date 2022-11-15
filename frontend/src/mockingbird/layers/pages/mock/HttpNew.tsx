import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import createStubStore from 'src/mockingbird/models/mockCreate/reducers/store';
import {
  createAction,
  resetCreateStateAction,
} from 'src/mockingbird/models/mockCreate/actions';
import FormHttp from './FormHttp';
import { mapFormDataToStub } from './utils';
import type { TFormCallback, THTTPFormData } from './types';

interface Props {
  labels: string[];
  serviceId: string;
}

export default function HttpNew({ labels, serviceId }: Props) {
  const create = useActions(createAction);
  const { status } = useStoreSelector(createStubStore, selectorAsIs);
  const resetCreateState = useActions(resetCreateStateAction);
  useEffect(() => resetCreateState, [resetCreateState]);

  const onCreate = useCallback(
    (data: THTTPFormData, callbacks: TFormCallback[]) => {
      create({
        type: 'http',
        data: mapFormDataToStub(data, callbacks, serviceId),
        serviceId,
      });
    },
    [serviceId, create]
  );

  return (
    <FormHttp
      labels={labels}
      serviceId={serviceId}
      submitDisabled={status === 'loading'}
      onSubmit={onCreate}
    />
  );
}
