import { createReducer, createEvent } from '@tramvai/state';
import type { Mock } from '../types';

export type MockState = {
  status:
    | 'none'
    | 'loading'
    | 'complete'
    | 'error'
    | 'updating'
    | 'updating-error'
    | 'deleting'
    | 'deleting-error';
  data?: Mock;
};

const storeName = 'mockState';
const initialState: MockState = {
  status: 'none',
};

export const fetchSuccess = createEvent<Mock>('FETCH_MOCK_SUCCESS');
export const fetchFail = createEvent('FETCH_MOCK_FAIL');
export const setLoading = createEvent('SET_LOADING_MOCK');
export const updateSuccess = createEvent<Mock>('UPDATE_MOCK_SUCCESS');
export const updateFail = createEvent<any>('UPDATE_MOCK_FAIL');
export const setUpdating = createEvent('SET_UPDATING_MOCK');
export const deleteSuccess = createEvent('DELETE_MOCK_SUCCESS');
export const deleteFail = createEvent<any>('DELETE_MOCK_FAIL');
export const setDeleting = createEvent('SET_DELETING_MOCK');
export const reset = createEvent('RESET_MOCK_STATE');

const reducer = createReducer(storeName, initialState)
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

reducer
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

reducer
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

reducer.on(reset, () => initialState);

export default reducer;
