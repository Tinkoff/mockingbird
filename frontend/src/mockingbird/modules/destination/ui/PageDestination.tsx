import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { useUrl } from '@tramvai/module-router';
import Loader from '@platform-ui/loader';
import PageHeader from 'src/components/PageHeader/PageHeader';
import Error from 'src/components/List/ListError';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import { getPathDestinations } from 'src/mockingbird/paths';
import Form from './Form';
import { store } from '../reducers';
import { fetchAction, updateAction, resetAction } from '../actions';
import { mapFormDataToDestination } from '../utils';
import type { DestinationFormData } from '../types';

export default function PageDestination() {
  const {
    query: { service: serviceId, destination: name },
  } = useUrl();
  const { status, data: destination } = useStoreSelector(store, selectorAsIs);
  const fetchDestination = useActions(fetchAction);
  const updateDestination = useActions(updateAction);
  const resetState = useActions(resetAction);
  useEffect(() => resetState, [resetState]);
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
      {status === 'loading' && <Loader size="xxl" centered />}
      {status === 'error' && <Error onRetry={handleFetchRetry} />}
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
