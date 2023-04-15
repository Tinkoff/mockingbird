import React, { useEffect, useCallback } from 'react';
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
import { getPathServices, getPathSourceNew } from 'src/mockingbird/paths';
import useService from 'src/mockingbird/modules/service';
import SourceItem from './SourceItem';
import sourcesStore from '../reducers';
import { fetchAction, resetAction } from '../actions';

export default function PageSources() {
  const url = useUrl();
  const serviceId = Array.isArray(url.query.service)
    ? url.query.service[0]
    : url.query.service;
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
  useEffect(() => resetSources as any, [resetSources]);
  const service = useService(serviceId);
  return (
    <Page>
      <PageHeader
        title={`Источники сервиса ${(service && service.name) || serviceId}`}
        backText="К списку сервисов"
        backPath={getPathServices()}
        right={
          <Button size="sm" onClick={navigateToCreate}>
            Создать
          </Button>
        }
      />
      {status === 'loading' && <ListLoading />}
      {status === 'error' && <ListError onRetry={handleRetry} />}
      {status === 'complete' && !sources.length && <ListEmpty />}
      {sources.length > 0 && (
        <List>
          {sources.map((item: any) => (
            <SourceItem key={item.name} item={item} serviceId={serviceId} />
          ))}
        </List>
      )}
    </Page>
  );
}
