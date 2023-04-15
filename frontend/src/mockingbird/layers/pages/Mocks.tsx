import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { useNavigate, useUrl } from '@tramvai/module-router';
import {
  Flex,
  Box,
  Space,
  Button,
  Select,
  MultiSelect,
  TextInput,
} from '@mantine/core';
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore for tree shaking purposes
import IconX from '@tabler/icons-react/dist/esm/icons/IconX';
import useDebounce from 'src/infrastructure/utils/hooks/debouce';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import useLabels from 'src/mockingbird/modules/labels';
import { mapSelectItem } from 'src/mockingbird/infrastructure/helpers/forms';
import type { MocksState } from 'src/mockingbird/models/mocks/reducers/store';
import mocksStore from 'src/mockingbird/models/mocks/reducers/store';
import {
  fetchAction,
  fetchMoreAction,
  setTypeAction,
  setQueryAction,
  setLabelsAction,
} from 'src/mockingbird/models/mocks/actions';
import useService from 'src/mockingbird/modules/service';
import { getPathServices, getPathMockNew } from 'src/mockingbird/paths';
import PageHeader from 'src/components/PageHeader/PageHeader';
import List from 'src/components/List/List';
import ListError from 'src/components/List/ListError';
import ListEmpty from 'src/components/List/ListEmpty';
import { ListLoading } from 'src/components/List/ListLoading';
import HTTPMock from './mocks/HTTPMock';
import ScenarioItem from './mocks/ScenarioItem';
import GRPCItem from './mocks/GRPCItem';
import styles from './common.css';

const TYPES = [
  {
    label: 'HTTP',
    value: 'http',
  },
  {
    label: 'Scenario',
    value: 'scenario',
  },
  {
    label: 'GRPC',
    value: 'grpc',
  },
];

const MOCK_ITEM_HEIGHT = 80;

export default function Mocks() {
  const url = useUrl();
  const serviceId = Array.isArray(url.query.service)
    ? url.query.service[0]
    : url.query.service;
  const navigateToCreate = useNavigate(getPathMockNew(serviceId));

  const service = useService(serviceId);
  const allLabels = useLabels(serviceId);

  const { status, mocks, type, query, labels, hasMore } = useStoreSelector(
    mocksStore,
    selectorAsIs as (state: MocksState) => MocksState
  );
  const fetchMocks = useActions(fetchAction);
  const fetchMoreMocks = useActions(fetchMoreAction);
  const setType = useActions(setTypeAction);
  const setQuery = useActions(setQueryAction);
  const setLabels = useActions(setLabelsAction);

  const queryDebounced = useDebounce<string>(query);
  useEffect(() => {
    if (!process.env.BROWSER) return;
    fetchMocks(serviceId);
  }, [queryDebounced, serviceId, fetchMocks]);

  const handleChangeType = useCallback(
    (value) => {
      setType(value);
    },
    [setType]
  );
  const handleChangeQuery = useCallback(
    (e) => {
      setQuery(e.currentTarget.value);
    },
    [setQuery]
  );
  const handleResetQuery = useCallback(() => {
    setQuery('');
  }, [setQuery]);
  const handleChangeLabels = useCallback(
    (value) => {
      setLabels(value);
    },
    [setLabels]
  );
  const handleRetry = useCallback(() => {
    fetchMocks();
  }, [fetchMocks]);
  const handleFetchMore = useCallback(() => {
    fetchMoreMocks();
  }, [fetchMoreMocks]);

  const ItemComponent = getItemComponent(type);
  return (
    <div className={styles.root}>
      <PageHeader
        title={(service && service.name) || serviceId}
        backText="К списку сервисов"
        backPath={getPathServices()}
        right={
          <Button size="sm" onClick={navigateToCreate}>
            Создать
          </Button>
        }
      />
      <TextInput
        placeholder="Поиск"
        size="md"
        value={query}
        onChange={handleChangeQuery}
        rightSection={
          query ? <IconX size="0.8rem" onClick={handleResetQuery} /> : null
        }
      />
      <Flex mb="xl">
        <Box sx={{ width: '50%' }}>
          <Select
            placeholder="Тип"
            size="md"
            data={TYPES}
            value={type}
            onChange={handleChangeType}
          />
        </Box>
        <Space w="md" />
        <Box sx={{ width: '50%' }}>
          <MultiSelect
            placeholder="Фильтр по лейблам"
            size="md"
            data={allLabels.map(mapSelectItem)}
            value={labels}
            onChange={handleChangeLabels}
          />
        </Box>
      </Flex>
      {status === 'loading' && <ListLoading mih={MOCK_ITEM_HEIGHT} />}
      {status === 'error' && <ListError onRetry={handleRetry} />}
      {status === 'complete' && !mocks.length && <ListEmpty />}
      {mocks && mocks.length > 0 && (
        <List>
          {mocks.map((i) => (
            <ItemComponent key={i.id} item={i as any} serviceId={serviceId} />
          ))}
        </List>
      )}
      {hasMore && (
        <div className={styles.buttonMore}>
          <Button
            size="md"
            variant="outline"
            disabled={status === 'loading-more'}
            onClick={handleFetchMore}
          >
            Загрузить еще
          </Button>
        </div>
      )}
    </div>
  );
}

function getItemComponent(type: string) {
  switch (type) {
    case 'http':
      return HTTPMock;
    case 'scenario':
      return ScenarioItem;
    case 'grpc':
      return GRPCItem;
    default:
      return () => null;
  }
}
