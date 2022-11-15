import { createReducer, createEvent } from '@tramvai/state';
import type { Mock } from '../../mock/types';

type Type = 'http' | 'scenario';
type MocksState = {
  status:
    | 'none'
    | 'loading'
    | 'complete'
    | 'error'
    | 'loading-more'
    | 'error-more';
  mocks: Mock[];
  type: Type;
  page: number;
  query: string;
  labels: string[];
  service: string;
  hasMore: boolean;
};

export const storeName = 'mocksState';
const initialState: MocksState = {
  status: 'none',
  mocks: [],
  type: 'http',
  page: 0,
  query: '',
  labels: [],
  service: '',
  hasMore: false,
};

const PAGE_LIMIT = 20;

export const fetchSuccess = createEvent<Mock[]>('FETCH_MOCKS_SUCCESS');
export const fetchFail = createEvent('FETCH_MOCKS_FAIL');
export const setLoading = createEvent('SET_LOADING_MOCKS');
export const fetchMoreSuccess = createEvent<Mock[]>('FETCH_MORE_MOCKS_SUCCESS');
export const fetchMoreFail = createEvent('FETCH_MORE_MOCKS_FAIL');
export const setMoreLoading = createEvent('SET_LOADING_MORE_MOCKS');
export const setType = createEvent<Type>('SET_TYPE_MOCKS');
export const setQuery = createEvent<string>('SET_QUERY_MOCKS');
export const setLabels = createEvent<string[]>('SET_LABELS_MOCKS');
export const setService = createEvent<string>('SET_SERVICE_MOCKS');

const reducer = createReducer(storeName, initialState);

export default reducer;

reducer
  .on(fetchSuccess, (state, mocks) => ({
    ...state,
    status: 'complete',
    mocks,
    hasMore: mocks.length === PAGE_LIMIT,
  }))
  .on(fetchFail, (state) => ({
    ...state,
    status: 'error',
  }))
  .on(setLoading, (state) => ({
    ...state,
    status: 'loading',
    mocks: [],
    hasMore: false,
  }));

reducer
  .on(fetchMoreSuccess, (state, mocks) => ({
    ...state,
    status: 'complete',
    mocks: [...state.mocks, ...mocks],
    hasMore: mocks.length === PAGE_LIMIT,
  }))
  .on(fetchMoreFail, (state) => ({
    ...state,
    status: 'error-more',
  }))
  .on(setMoreLoading, (state) => ({
    ...state,
    page: state.status === 'complete' ? state.page + 1 : state.page,
    status: 'loading-more',
  }));

reducer.on(setType, (state, type) => ({
  ...state,
  page: 0,
  type,
}));

reducer.on(setQuery, (state, query) => ({
  ...state,
  page: 0,
  query,
}));

reducer.on(setLabels, (state, labels) => ({
  ...state,
  page: 0,
  labels,
}));

reducer.on(setService, (state, service) => {
  if (state.service === service) return state;
  return {
    ...initialState,
    service,
  };
});
