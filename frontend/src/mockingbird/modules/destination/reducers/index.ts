import { createReducer, createEvent } from '@tramvai/state';
import type { Destination } from '../types';

export type CreateDestinationState = {
  status: 'none' | 'loading' | 'complete' | 'error';
};

const initialCreateState: CreateDestinationState = {
  status: 'none',
};

export const createSuccess = createEvent('CREATE_DESTINATION_SUCCESS');
export const createFail = createEvent('CREATE_DESTINATION_FAIL');
export const setCreating = createEvent('SET_CREATING_DESTINATION');

export const createStore = createReducer(
  'createDestinationState',
  initialCreateState
)
  .on(createSuccess, () => ({
    status: 'complete',
  }))
  .on(createFail, () => ({
    status: 'error',
  }))
  .on(setCreating, () => ({
    status: 'loading',
  }));

export type StoreState = {
  status:
    | 'none'
    | 'loading'
    | 'complete'
    | 'error'
    | 'updating'
    | 'updating-error';
  data?: Destination;
};

const initialState: StoreState = {
  status: 'none',
};

export const fetchSuccess = createEvent<Destination>(
  'FETCH_DESTINATION_SUCCESS'
);
export const fetchFail = createEvent('FETCH_DESTINATION_FAIL');
export const setFetching = createEvent('SET_FETCHING_DESTINATION');
export const updateSuccess = createEvent<Destination>(
  'UPDATE_DESTINATION_SUCCESS'
);
export const updateFail = createEvent<any>('UPDATE_DESTINATION_FAIL');
export const setUpdating = createEvent('SET_UPDATING_DESTINATION');
export const reset = createEvent('RESET_DESTINATION_STATE');

export const store = createReducer('destinationState', initialState)
  .on(fetchSuccess, (state, data) => ({
    data,
    status: 'complete',
  }))
  .on(fetchFail, (state) => ({
    ...state,
    status: 'error',
  }))
  .on(setFetching, (state) => ({
    ...state,
    status: 'loading',
  }));

store
  .on(updateSuccess, (state, data) => ({
    data,
    status: 'complete',
  }))
  .on(updateFail, (state) => ({
    ...state,
    status: 'updating-error',
  }))
  .on(setUpdating, (state) => ({
    ...state,
    status: 'updating',
  }));

store.on(reset, () => initialState);
