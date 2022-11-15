import { createReducer, createEvent } from '@tramvai/state';

type LabelsState = {
  status: 'none' | 'loading' | 'complete' | 'error';
  labels: string[];
};

const storeName = 'labelsState';
const initialState: LabelsState = {
  status: 'none',
  labels: [],
};

export const fetchSuccess = createEvent<string[]>('FETCH_LABELS_SUCCESS');
export const fetchFail = createEvent('FETCH_LABELS_FAIL');
export const setLoading = createEvent('SET_LOADING_LABELS');

export default createReducer(storeName, initialState)
  .on(fetchSuccess, (state, labels) => ({
    labels,
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
