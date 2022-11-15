import React, { useCallback, useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { useNavigate, useUrl } from '@tramvai/module-router';
import Button from '@platform-ui/button';
import Input from '@platform-ui/input';
import Loader from '@platform-ui/loader';
import Select, { MultiselectTagged } from '@platform-ui/select';
import useDebounce from 'src/infrastructure/utils/hooks/debouce';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import useLabels from 'src/mockingbird/modules/labels';
import { mapSelectItem } from 'src/mockingbird/infrastructure/helpers/forms';
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
import HTTPMock from './mocks/HTTPMock';
import ScenarioItem from './mocks/ScenarioItem';
import GRPCItem from './mocks/GRPCItem';
import styles from './common.css';

const TYPES = [
  {
    title: 'HTTP',
    value: 'http',
  },
  {
    title: 'Scenario',
    value: 'scenario',
  },
  {
    title: 'GRPC',
    value: 'grpc',
  },
];

export default function Mocks() {
  const {
    query: { service: serviceId },
  } = useUrl();
  const navigateToCreate = useNavigate(getPathMockNew(serviceId));

  const service = useService(serviceId);
  const allLabels = useLabels(serviceId);

  const { status, mocks, type, query, labels, hasMore } = useStoreSelector(
    mocksStore,
    selectorAsIs
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
    (_, { value }) => {
      setType(value);
    },
    [setType]
  );
  const handleChangeQuery = useCallback(
    (_, { value }) => {
      setQuery(value);
    },
    [setQuery]
  );
  const handleChangeLabels = useCallback(
    (_, { value }) => {
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
          <Button size="m" onClick={navigateToCreate}>
            Создать
          </Button>
        }
      />
      <Input
        label="Поиск"
        size="m"
        value={query}
        onChange={handleChangeQuery}
        cleanable
      />
      <div className={styles.row}>
        <Select
          label="Тип"
          size="m"
          options={TYPES}
          value={type}
          onChange={handleChangeType}
        />
        <MultiselectTagged
          placeholder="Фильтр по лейблам"
          size="m"
          searchThreshold={2}
          options={allLabels.map(mapSelectItem)}
          value={labels}
          onChange={handleChangeLabels}
        />
      </div>
      {status === 'loading' && <Loader size="xxl" centered />}
      {status === 'error' && <ListError onRetry={handleRetry} />}
      {status === 'complete' && !mocks.length && <ListEmpty />}
      {mocks && mocks.length > 0 && (
        <List>
          {mocks.map((i) => (
            <ItemComponent key={i.id} item={i} serviceId={serviceId} />
          ))}
        </List>
      )}
      {hasMore && (
        <div className={styles.buttonMore}>
          <Button
            size="m"
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
