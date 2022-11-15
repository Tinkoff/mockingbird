import type { ActionContext } from '@tramvai/core';
import { createAction } from '@tramvai/core';
import { addQuery } from '@tinkoff/url';
import { getJson } from 'src/mockingbird/infrastructure/request';
import {
  setLoading,
  fetchSuccess,
  fetchFail,
  setMoreLoading,
  fetchMoreSuccess,
  fetchMoreFail,
  setType,
  setQuery,
  setLabels,
  setService,
  storeName,
} from '../reducers/store';

const fetchActionFn = fetchActionCreatorFn.bind(null, {
  loading: setLoading,
  success: fetchSuccess,
  fail: fetchFail,
});

export const fetchAction = createAction({
  name: 'FETCH_MOCKS_ACTION',
  fn: fetchActionFn,
});

export const fetchMoreAction = createAction({
  name: 'FETCH_MORE_MOCKS_ACTION',
  fn: fetchActionCreatorFn.bind(null, {
    loading: setMoreLoading,
    success: fetchMoreSuccess,
    fail: fetchMoreFail,
  }),
});

export const setTypeAction = createAction({
  name: 'SET_TYPE_MOCKS_ACTION',
  fn: (store, type) => {
    const { dispatch, getState } = store;
    const {
      [storeName]: { type: currentType },
    } = getState();
    if (currentType === type) return;
    return dispatch(setType(type)).then(() => fetchActionFn(store));
  },
});

export const setQueryAction = createAction({
  name: 'SET_QUERY_MOCKS_ACTION',
  fn: ({ dispatch }, query) => dispatch(setQuery(query)),
});

export const setLabelsAction = createAction({
  name: 'SET_LABELS_MOCKS_ACTION',
  fn: (store, labels) => {
    const { dispatch } = store;
    return dispatch(setLabels(labels)).then(() => fetchActionFn(store));
  },
});

function fetchActionCreatorFn(
  actions: any,
  store: ActionContext,
  serviceId?: string
) {
  const { loading, success, fail } = actions;
  const { dispatch, getState } = store;
  if (typeof serviceId === 'string') dispatch(setService(serviceId));
  dispatch(loading());
  const {
    environment: { MOCKINGBIRD_API },
    [storeName]: { type, page, query, labels, service },
  } = getState();
  const method = getMethodByType(type);
  const queryParams = { service, page, query, labels };
  const url = addQuery(`${MOCKINGBIRD_API}/v2/${method}`, queryParams).href;
  return getJson(url)
    .then((response) => {
      if (!response || !Array.isArray(response)) {
        throw new Error();
      }
      return dispatch(success(response));
    })
    .catch(() => dispatch(fail()));
}

function getMethodByType(method: string) {
  switch (method) {
    case 'http':
      return 'stub';
    case 'scenario':
      return 'scenario';
    case 'grpc':
      return 'grpcStub';
    default:
      return '';
  }
}
