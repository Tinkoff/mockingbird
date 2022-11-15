import { createReducer, createEvent } from '@tramvai/state';
import type { Destination } from '../types';

type State = {
  status: 'none' | 'loading' | 'complete' | 'error';
  destinations: Destination[];
};

const storeName = 'destinationsState';
const initialState: State = {
  status: 'none',
  destinations: [],
};

export const fetchSuccess = createEvent<Destination[]>(
  'FETCH_DESTINATIONS_SUCCESS'
);
export const fetchFail = createEvent('FETCH_DESTINATIONS_FAIL');
export const reset = createEvent('RESET_DESTINATIONS');
export const setLoading = createEvent('SET_LOADING_DESTINATIONS');

export default createReducer(storeName, initialState)
  .on(fetchSuccess, (state, destinations) => ({
    destinations,
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
