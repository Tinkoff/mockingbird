import React, { useCallback } from 'react';
import { useUrl } from '@tramvai/module-router';
import { useActions, useStoreSelector } from '@tramvai/state';
import PageHeader from 'src/components/PageHeader/PageHeader';
import Page from 'src/mockingbird/components/Page';
import { getPathSources } from 'src/mockingbird/paths';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import Form from './Form';
import { createAction } from '../actions';
import { createStore } from '../reducers';
import { mapFormDataToSource } from '../utils';
import type { SourceFormData } from '../types';

export default function MockNew() {
  const {
    query: { service: serviceId },
  } = useUrl();
  const create = useActions(createAction);
  const { status } = useStoreSelector(createStore, selectorAsIs);
  const onCreate = useCallback(
    (data: SourceFormData) => {
      create({
        data: mapFormDataToSource(data, serviceId),
        serviceId,
      });
    },
    [serviceId, create]
  );
  return (
    <Page>
      <PageHeader
        title="Создание источника"
        backText="К списку источников"
        backPath={getPathSources(serviceId)}
      />
      <Form disabled={status === 'loading'} onSubmit={onCreate} />
    </Page>
  );
}
