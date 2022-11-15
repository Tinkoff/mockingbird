import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import createStubStore from 'src/mockingbird/models/mockCreate/reducers/store';
import {
  createAction,
  resetCreateStateAction,
} from 'src/mockingbird/models/mockCreate/actions';
import FormScenario from './FormScenario';
import { mapFormDataToScenario } from './utils';
import type { TFormCallback, TScenarioFormData } from './types';

interface Props {
  labels: string[];
  serviceId: string;
}

export default function ScenarioNew({ labels, serviceId }: Props) {
  const create = useActions(createAction);
  const { status } = useStoreSelector(createStubStore, selectorAsIs);
  const resetCreateState = useActions(resetCreateStateAction);
  useEffect(() => resetCreateState, [resetCreateState]);

  const onCreate = useCallback(
    (data: TScenarioFormData, callbacks: TFormCallback[]) => {
      create({
        type: 'scenario',
        data: mapFormDataToScenario(data, callbacks, serviceId),
        serviceId,
      });
    },
    [serviceId, create]
  );

  return (
    <FormScenario
      labels={labels}
      serviceId={serviceId}
      submitDisabled={status === 'loading'}
      onSubmit={onCreate}
    />
  );
}
