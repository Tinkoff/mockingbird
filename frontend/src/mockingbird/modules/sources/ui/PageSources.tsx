import React, { useEffect, useCallback } from 'react';
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
import { getPathServices, getPathSourceNew } from 'src/mockingbird/paths';
import useService from 'src/mockingbird/modules/service';
import SourceItem from './SourceItem';
import sourcesStore from '../reducers';
import { fetchAction, resetAction } from '../actions';

export default function PageSources() {
  const {
    query: { service: serviceId },
  } = useUrl();
  const navigateToCreate = useNavigate(getPathSourceNew(serviceId));
  const fetchSources = useActions(fetchAction);
  const resetSources = useActions(resetAction);
  const { sources, status } = useStoreSelector(sourcesStore, selectorAsIs);
  useEffect(() => {
    if (!process.env.BROWSER) return;
    fetchSources(serviceId);
  }, [fetchSources, serviceId]);
  const handleRetry = useCallback(() => {
    fetchSources(serviceId);
  }, [fetchSources, serviceId]);
  useEffect(() => resetSources, [resetSources]);
  const service = useService(serviceId);
  return (
    <Page>
      <PageHeader
        title={`Источники сервиса ${(service && service.name) || serviceId}`}
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
      {status === 'complete' && !sources.length && <ListEmpty />}
      {sources.length > 0 && (
        <List>
          {sources.map((item) => (
            <SourceItem key={item.name} item={item} serviceId={serviceId} />
          ))}
        </List>
      )}
    </Page>
  );
}
