import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { useUrl } from '@tramvai/module-router';
import { Paper, Text, Title, Button } from '@mantine/core';
import PageHeader from 'src/components/PageHeader/PageHeader';
import Error from 'src/components/List/ListError';
import { ListLoading } from 'src/components/List/ListLoading';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import { getPathSources } from 'src/mockingbird/paths';
import Form from './Form';
import type { StoreState } from '../reducers';
import { store } from '../reducers';
import {
  fetchAction,
  updateAction,
  deleteAction,
  resetAction,
} from '../actions';
import { mapFormDataToSource } from '../utils';
import type { SourceFormData } from '../types';

const SOURCE_HEIGHT_ITEM = 80;

export default function PageSource() {
  const url = useUrl();
  const serviceId = Array.isArray(url.query.service)
    ? url.query.service[0]
    : url.query.service;
  const name = Array.isArray(url.query.source)
    ? url.query.source[0]
    : url.query.source;
  const basePath = getPathSources(serviceId);
  const { status, data: source } = useStoreSelector(
    store,
    selectorAsIs as (s: StoreState) => StoreState
  );
  const fetchSource = useActions(fetchAction);
  const updateSource = useActions(updateAction);
  const deleteSource = useActions(deleteAction);
  const resetState = useActions(resetAction);
  useEffect(() => resetState as any, [resetState]);
  useEffect(() => {
    fetchSource({ name });
  }, [fetchSource, name]);
  const handleFetchRetry = useCallback(() => {
    fetchSource({ name });
  }, [fetchSource, name]);
  const onUpdate = useCallback(
    (data: SourceFormData) => {
      updateSource({
        name,
        data: mapFormDataToSource(data, serviceId),
      });
    },
    [updateSource, serviceId, name]
  );
  const onDelete = useCallback(() => {
    deleteSource({
      name,
      basePath,
    });
  }, [deleteSource, name, basePath]);
  const actions = (
    <Paper>
      <Title order={4}>Удалить навсегда</Title>
      <Text size="md" mb="lg">
        Источник будет немедленно удален, действие необратимо
      </Text>
      <Button
        size="md"
        variant="outline"
        disabled={status === 'deleting'}
        onClick={onDelete}
      >
        Удалить
      </Button>
    </Paper>
  );
  return (
    <div>
      <PageHeader
        title="Редактирование источника"
        backText="К списку источников"
        backPath={basePath}
      />
      {status === 'loading' && <ListLoading mih={SOURCE_HEIGHT_ITEM} />}
      {status === 'error' && <Error onRetry={handleFetchRetry} />}
      {source && (
        <Form
          actions={actions}
          data={source}
          submitText="Сохранить"
          disabled={status === 'updating'}
          onSubmit={onUpdate}
        />
      )}
    </div>
  );
}
