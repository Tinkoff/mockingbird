import { createReducer, createEvent } from '@tramvai/state';
import type { Service } from '../types';

export type ServiceState = {
  status: 'none' | 'loading' | 'complete' | 'error';
  data?: Service;
};

const storeName = 'serviceState';
const initialState: ServiceState = {
  status: 'none',
};

export const fetchSuccess = createEvent<Service>('FETCH_SERVICE_SUCCESS');
export const fetchFail = createEvent('FETCH_SERVICE_FAIL');
export const setLoading = createEvent('SET_LOADING_SERVICE');

export default createReducer(storeName, initialState)
  .on(fetchSuccess, (state, data) => ({
    data,
    status: 'complete',
  }))
  .on(fetchFail, (state) => ({
    ...state,
    status: 'error',
  }))
  .on(setLoading, (state) => ({
    ...state,
    status: 'loading',
  }));
