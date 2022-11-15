import { createReducer, createEvent } from '@tramvai/state';
import type { Service } from '../../service/types';

type ServicesState = {
  status: 'none' | 'loading' | 'complete' | 'error';
  services: Service[];
};

const storeName = 'servicesState';
const initialState: ServicesState = {
  status: 'none',
  services: [],
};

export const fetchSuccess = createEvent<Service[]>('FETCH_SERVICES_SUCCESS');
export const fetchFail = createEvent('FETCH_SERVICES_FAIL');
export const setLoading = createEvent('SET_LOADING_SERVICES');

export default createReducer(storeName, initialState)
  .on(fetchSuccess, (state, services) => ({
    services,
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
