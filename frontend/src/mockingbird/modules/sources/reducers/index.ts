import { createReducer, createEvent } from '@tramvai/state';
import type { Source } from '../types';

type State = {
  status: 'none' | 'loading' | 'complete' | 'error';
  sources: Source[];
};

const storeName = 'sourcesState';
const initialState: State = {
  status: 'none',
  sources: [],
};

export const fetchSuccess = createEvent<Source[]>('FETCH_SOURCES_SUCCESS');
export const fetchFail = createEvent('FETCH_SOURCES_FAIL');
export const reset = createEvent('RESET_SOURCES');
export const setLoading = createEvent('SET_LOADING_SOURCES');

export default createReducer(storeName, initialState)
  .on(fetchSuccess, (state, sources) => ({
    sources,
    status: 'complete',
  }))
  .on(fetchFail, (state) => ({
    ...state,
    status: 'error',
  }))
  .on(reset, () => initialState)
  .on(setLoading, (state) => ({
    ...state,
    status: 'loading',
  }));
