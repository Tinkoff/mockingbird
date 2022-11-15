import { createReducer, createEvent } from '@tramvai/state';
import type { Source } from '../types';

export type CreateSourceState = {
  status: 'none' | 'loading' | 'complete' | 'error';
};

const initialCreateState: CreateSourceState = {
  status: 'none',
};

export const createSuccess = createEvent('CREATE_SOURCE_SUCCESS');
export const createFail = createEvent('CREATE_SOURCE_FAIL');
export const setCreating = createEvent('SET_CREATING_SOURCE');

export const createStore = createReducer(
  'createSourceState',
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
    | 'updating-error'
    | 'deleting'
    | 'deleting-error';
  data?: Source;
};

const initialState: StoreState = {
  status: 'none',
};

export const fetchSuccess = createEvent<Source>('FETCH_SOURCE_SUCCESS');
export const fetchFail = createEvent('FETCH_SOURCE_FAIL');
export const setFetching = createEvent('SET_FETCHING_SOURCE');
export const updateSuccess = createEvent<Source>('UPDATE_SOURCE_SUCCESS');
export const updateFail = createEvent<any>('UPDATE_SOURCE_FAIL');
export const setUpdating = createEvent('SET_UPDATING_SOURCE');
export const deleteSuccess = createEvent('DELETE_SOURCE_SUCCESS');
export const deleteFail = createEvent<any>('DELETE_SOURCE_FAIL');
export const setDeleting = createEvent('SET_DELETING_SOURCE');
export const reset = createEvent('RESET_SOURCE_STATE');

export const store = createReducer('sourceState', initialState)
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

store
  .on(deleteSuccess, (state) => ({
    ...state,
    status: 'complete',
  }))
  .on(deleteFail, (state) => ({
    ...state,
    status: 'deleting-error',
  }))
  .on(setDeleting, (state) => ({
    ...state,
    status: 'deleting',
  }));

store.on(reset, () => initialState);
