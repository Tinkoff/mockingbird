import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { useUrl } from '@tramvai/module-router';
import Button from '@platform-ui/button';
import Island from '@platform-ui/island';
import Loader from '@platform-ui/loader';
import Text from '@platform-ui/text';
import PageHeader from 'src/components/PageHeader/PageHeader';
import Error from 'src/components/List/ListError';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import useLabels from 'src/mockingbird/modules/labels';
import mockStore from 'src/mockingbird/models/mock/reducers/store';
import {
  fetchAction,
  updateAction,
  deleteAction,
  resetMockStateAction,
} from 'src/mockingbird/models/mock/actions';
import { getPathMocks } from 'src/mockingbird/paths';
import FormHttp from './mock/FormHttp';
import FormScenario from './mock/FormScenario';
import FormGrpc from './mock/FormGrpc';
import { mapFormDataToStub, mapFormDataToScenario } from './mock/utils';
import type {
  THTTPFormData,
  TFormCallback,
  TScenarioFormData,
} from './mock/types';

const TYPES = ['http', 'scenario', 'grpc'];

export default function Mock() {
  const {
    query: { service: serviceId, mock: mockId, type },
  } = useUrl();
  const basePath = getPathMocks(serviceId);
  const labels = useLabels(serviceId);

  const { status, data: mock } = useStoreSelector(mockStore, selectorAsIs);
  const fetchMock = useActions(fetchAction);
  const updateMock = useActions(updateAction);
  const deleteMock = useActions(deleteAction);
  const resetState = useActions(resetMockStateAction);
  useEffect(() => resetState, [resetState]);
  useEffect(() => {
    fetchMock({
      id: mockId,
      type,
    });
  }, [mockId, type, fetchMock]);
  const handleFetchRetry = useCallback(() => {
    if (!process.env.BROWSER) return;
    fetchMock({
      id: mockId,
      type,
    });
  }, [mockId, type, fetchMock]);
  const onUpdate = useCallback(
    (data: THTTPFormData | TScenarioFormData, callbacks: TFormCallback[]) => {
      const map = type === 'http' ? mapFormDataToStub : mapFormDataToScenario;
      updateMock({
        id: mockId,
        type,
        data: map(data, callbacks, serviceId),
      });
    },
    [serviceId, mockId, type, updateMock]
  );
  const onDelete = useCallback(() => {
    deleteMock({
      id: mockId,
      type,
      basePath,
    });
  }, [basePath, mockId, type, deleteMock]);

  if (!process.env.BROWSER)
    return (
      <div>
        <PageHeader
          title="Редактирование"
          backText="К списку моков"
          backPath={basePath}
        />
      </div>
    );

  const pageHeader = (
    <PageHeader
      title={formatTitle(type)}
      backText="К списку моков"
      backPath={basePath}
    />
  );
  const actions = (
    <Island
      title="Удалить навсегда"
      text="Мок будет немедленно удален, действие необратимо"
      flatCorners="true"
      side={
        <Button size="m" disabled={status === 'deleting'} onClick={onDelete}>
          Удалить
        </Button>
      }
    />
  );
  if (!TYPES.includes(type))
    return (
      <div>
        {pageHeader}
        <Text size={15} color="red">
          Неизвестный тип
        </Text>
      </div>
    );
  return (
    <div>
      {pageHeader}
      {status === 'loading' && <Loader size="xxl" centered />}
      {status === 'error' && <Error onRetry={handleFetchRetry} />}
      {type === 'http' && mock && (
        <FormHttp
          labels={labels}
          serviceId={serviceId}
          data={mock}
          actions={actions}
          submitText="Сохранить"
          submitDisabled={status === 'updating'}
          onSubmit={onUpdate}
        />
      )}
      {type === 'scenario' && mock && (
        <FormScenario
          labels={labels}
          serviceId={serviceId}
          data={mock}
          actions={actions}
          submitText="Сохранить"
          submitDisabled={status === 'updating'}
          onSubmit={onUpdate}
        />
      )}
      {type === 'grpc' && mock && (
        <FormGrpc
          labels={labels}
          serviceId={serviceId}
          data={mock}
          actions={actions}
          submitText="Сохранить"
          submitDisabled
          disabled
          onSubmit={onUpdate}
        />
      )}
    </div>
  );
}

function formatTitle(type) {
  switch (type) {
    case 'http':
      return 'Редактирование HTTP';
    case 'scenario':
      return 'Редактирование scenario';
    case 'grpc':
      return 'Просмотр GRPC';
    default:
      return 'Редактирование';
  }
}
