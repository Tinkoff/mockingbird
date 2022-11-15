import { createReducer, createEvent } from '@tramvai/state';

export type CreateMockState = {
  status: 'none' | 'loading' | 'complete' | 'error';
};

const storeName = 'createMockState';
const initialState: CreateMockState = {
  status: 'none',
};

export const createSuccess = createEvent('CREATE_MOCK_SUCCESS');
export const createFail = createEvent('CREATE_MOCK_FAIL');
export const setLoading = createEvent('SET_LOADING_MOCK');
export const reset = createEvent('RESET_CREATE_MOCK');

const reducer = createReducer(storeName, initialState)
  .on(createSuccess, () => ({
    status: 'complete',
  }))
  .on(createFail, () => ({
    status: 'error',
  }))
  .on(setLoading, () => ({
    status: 'loading',
  }));

reducer.on(reset, () => initialState);

export default reducer;
