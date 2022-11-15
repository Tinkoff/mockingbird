import { createAction as createActionCore } from '@tramvai/core';
import { getJson } from 'src/mockingbird/infrastructure/request';
import {
  setLoading,
  createSuccess,
  createFail,
  reset,
} from '../reducers/createStore';

export const createAction = createActionCore({
  name: 'CREATE_SERVICE_ACTION',
  fn: ({ dispatch, getState }, body) => {
    dispatch(setLoading());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v2/service`, {
      httpMethod: 'post',
      body,
    })
      .then((response) => {
        const event =
          response.status === 'success' && response.id
            ? createSuccess(response.id)
            : createFail(null);
        return dispatch(event);
      })
      .catch((e) => dispatch(createFail(e)));
  },
});

export const resetCreateStateAction = createActionCore({
  name: 'RESET_CREATE_SERVICE_ACTION',
  fn: ({ dispatch }) => dispatch(reset()),
});
