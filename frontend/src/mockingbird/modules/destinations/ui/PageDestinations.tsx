import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { useUrl, useNavigate } from '@tramvai/module-router';
import Button from '@platform-ui/button';
import Loader from '@platform-ui/loader';
import PageHeader from 'src/components/PageHeader/PageHeader';
import List from 'src/components/List/List';
import ListError from 'src/components/List/ListError';
import ListEmpty from 'src/components/List/ListEmpty';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import Page from 'src/mockingbird/components/Page';
import { getPathServices, getPathDestinationNew } from 'src/mockingbird/paths';
import useService from 'src/mockingbird/modules/service';
import DestinationItem from './DestinationItem';
import store from '../reducers';
import { fetchAction, resetAction } from '../actions';

export default function PageDestinations() {
  const {
    query: { service: serviceId },
  } = useUrl();
  const navigateToCreate = useNavigate(getPathDestinationNew(serviceId));
  const fetchDestinations = useActions(fetchAction);
  const resetDestinations = useActions(resetAction);
  const { destinations, status } = useStoreSelector(store, selectorAsIs);
  useEffect(() => {
    if (!process.env.BROWSER) return;
    fetchDestinations(serviceId);
  }, [fetchDestinations, serviceId]);
  const handleRetry = useCallback(() => {
    fetchDestinations(serviceId);
  }, [fetchDestinations, serviceId]);
  useEffect(() => resetDestinations, [resetDestinations]);
  const service = useService(serviceId);
  return (
    <Page>
      <PageHeader
        title={`Получатели сервиса ${(service && service.name) || serviceId}`}
        backText="К списку сервисов"
        backPath={getPathServices()}
        right={
          <Button size="m" onClick={navigateToCreate}>
            Создать
          </Button>
        }
      />
      {status === 'loading' && <Loader size="xxl" centered />}
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
