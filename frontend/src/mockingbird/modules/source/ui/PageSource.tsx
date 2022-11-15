import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { useUrl } from '@tramvai/module-router';
import Button from '@platform-ui/button';
import Island from '@platform-ui/island';
import Loader from '@platform-ui/loader';
import PageHeader from 'src/components/PageHeader/PageHeader';
import Error from 'src/components/List/ListError';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import { getPathSources } from 'src/mockingbird/paths';
import Form from './Form';
import { store } from '../reducers';
import {
  fetchAction,
  updateAction,
  deleteAction,
  resetAction,
} from '../actions';
import { mapFormDataToSource } from '../utils';
import type { SourceFormData } from '../types';

export default function PageSource() {
  const {
    query: { service: serviceId, source: name },
  } = useUrl();
  const basePath = getPathSources(serviceId);
  const { status, data: source } = useStoreSelector(store, selectorAsIs);
  const fetchSource = useActions(fetchAction);
  const updateSource = useActions(updateAction);
  const deleteSource = useActions(deleteAction);
  const resetState = useActions(resetAction);
  useEffect(() => resetState, [resetState]);
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
    <Island
      title="Удалить навсегда"
      text="Источник будет немедленно удален, действие необратимо"
      flatCorners="true"
      side={
        <Button size="m" disabled={status === 'deleting'} onClick={onDelete}>
          Удалить
        </Button>
      }
    />
  );
  return (
    <div>
      <PageHeader
        title="Редактирование источника"
        backText="К списку источников"
        backPath={basePath}
      />
      {status === 'loading' && <Loader size="xxl" centered />}
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
