import { createAction as createActionCore } from '@tramvai/core';
import { addToast, removeToast } from './store';

export const addToastAction = createActionCore({
  name: 'ADD_TOAST_ACTION',
  fn: ({ dispatch }, item) => dispatch(addToast(item)),
});

export const removeToastAction = createActionCore({
  name: 'REMOVE_TOAST_ACTION',
  fn: ({ dispatch }, item) => dispatch(removeToast(item)),
});
