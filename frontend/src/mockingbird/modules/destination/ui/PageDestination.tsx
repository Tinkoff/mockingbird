import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { useUrl } from '@tramvai/module-router';
import PageHeader from 'src/components/PageHeader/PageHeader';
import ListError from 'src/components/List/ListError';
import { ListLoading } from 'src/components/List/ListLoading';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import { getPathDestinations } from 'src/mockingbird/paths';
import Form from './Form';
import type { StoreState } from '../reducers';
import { store } from '../reducers';
import { fetchAction, updateAction, resetAction } from '../actions';
import { mapFormDataToDestination } from '../utils';
import type { DestinationFormData } from '../types';

const DESTINATION_ITEM_HEIGHT = 80;

export default function PageDestination() {
  const url = useUrl();
  const serviceId = Array.isArray(url.query.service)
    ? url.query.service[0]
    : url.query.service;
  const name = Array.isArray(url.query.destination)
    ? url.query.destination[0]
    : url.query.destination;
  const { status, data: destination } = useStoreSelector(
    store,
    selectorAsIs as (s: StoreState) => StoreState
  );
  const fetchDestination = useActions(fetchAction);
  const updateDestination = useActions(updateAction);
  const resetState = useActions(resetAction);
  useEffect(() => resetState as any, [resetState]);
  useEffect(() => {
    fetchDestination({ name });
  }, [fetchDestination, name]);
  const handleFetchRetry = useCallback(() => {
    fetchDestination({ name });
  }, [fetchDestination, name]);
  const onUpdate = useCallback(
    (data: DestinationFormData) => {
      updateDestination({
        name,
        data: mapFormDataToDestination(data, serviceId),
      });
    },
    [updateDestination, serviceId, name]
  );
  return (
    <div>
      <PageHeader
        title="Редактирование получателя"
        backText="К списку получателей"
        backPath={getPathDestinations(serviceId)}
      />
      {status === 'loading' && <ListLoading mih={DESTINATION_ITEM_HEIGHT} />}
      {status === 'error' && <ListError onRetry={handleFetchRetry} />}
      {destination && (
        <Form
          data={destination}
          submitText="Сохранить"
          disabled={status === 'updating'}
          onSubmit={onUpdate}
        />
      )}
    </div>
  );
}
