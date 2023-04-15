import React, { useCallback } from 'react';
import { useUrl } from '@tramvai/module-router';
import { useActions, useStoreSelector } from '@tramvai/state';
import PageHeader from 'src/components/PageHeader/PageHeader';
import Page from 'src/mockingbird/components/Page';
import { getPathDestinations } from 'src/mockingbird/paths';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import Form from './Form';
import { createAction } from '../actions';
import { createStore } from '../reducers';
import { mapFormDataToDestination } from '../utils';
import type { DestinationFormData } from '../types';

export default function MockNew() {
  const url = useUrl();
  const serviceId = Array.isArray(url.query.service)
    ? url.query.service[0]
    : url.query.service;
  const create = useActions(createAction);
  const { status } = useStoreSelector(createStore, selectorAsIs);
  const onCreate = useCallback(
    (data: DestinationFormData) => {
      create({
        data: mapFormDataToDestination(data, serviceId),
        serviceId,
      });
    },
    [serviceId, create]
  );
  return (
    <Page>
      <PageHeader
        title="Создание получателя"
        backText="К списку получателей"
        backPath={getPathDestinations(serviceId)}
      />
      <Form disabled={status === 'loading'} onSubmit={onCreate} />
    </Page>
  );
}
