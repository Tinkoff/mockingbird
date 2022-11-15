import { createAction as createActionCore } from '@tramvai/core';
import { addToast } from './store';

export const addToastAction = createActionCore({
  name: 'ADD_TOAST_ACTION',
  fn: ({ dispatch }, item) => dispatch(addToast(item)),
});
