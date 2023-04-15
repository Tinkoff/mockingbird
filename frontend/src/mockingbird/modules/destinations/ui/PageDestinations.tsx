import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { useUrl, useNavigate } from '@tramvai/module-router';
import { Button } from '@mantine/core';
import PageHeader from 'src/components/PageHeader/PageHeader';
import List from 'src/components/List/List';
import ListError from 'src/components/List/ListError';
import ListEmpty from 'src/components/List/ListEmpty';
import { ListLoading } from 'src/components/List/ListLoading';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import Page from 'src/mockingbird/components/Page';
import { getPathServices, getPathDestinationNew } from 'src/mockingbird/paths';
import useService from 'src/mockingbird/modules/service';
import DestinationItem from './DestinationItem';
import type { State } from '../reducers';
import store from '../reducers';
import { fetchAction, resetAction } from '../actions';

const DESTINATION_ITEM_HEIGHT = 80;

export default function PageDestinations() {
  const url = useUrl();
  const serviceId = Array.isArray(url.query.service)
    ? url.query.service[0]
    : url.query.service;
  const navigateToCreate = useNavigate(getPathDestinationNew(serviceId));
  const fetchDestinations = useActions(fetchAction);
  const resetDestinations = useActions(resetAction);
  const { destinations, status } = useStoreSelector(
    store,
    selectorAsIs as (s: State) => State
  );
  useEffect(() => {
    if (!process.env.BROWSER) return;
    fetchDestinations(serviceId);
  }, [fetchDestinations, serviceId]);
  const handleRetry = useCallback(() => {
    fetchDestinations(serviceId);
  }, [fetchDestinations, serviceId]);
  useEffect(() => resetDestinations as any, [resetDestinations]);
  const service = useService(serviceId);
  return (
    <Page>
      <PageHeader
        title={`Получатели сервиса ${(service && service.name) || serviceId}`}
        backText="К списку сервисов"
        backPath={getPathServices()}
        right={
          <Button size="sm" onClick={navigateToCreate}>
            Создать
          </Button>
        }
      />
      {status === 'loading' && <ListLoading mih={DESTINATION_ITEM_HEIGHT} />}
      {status === 'error' && <ListError onRetry={handleRetry} />}
      {status === 'complete' && !destinations.length && <ListEmpty />}
      {destinations.length > 0 && (
        <List>
          {destinations.map((item) => (
            <DestinationItem
              key={item.name}
              item={item}
              serviceId={serviceId}
            />
          ))}
        </List>
      )}
    </Page>
  );
}
