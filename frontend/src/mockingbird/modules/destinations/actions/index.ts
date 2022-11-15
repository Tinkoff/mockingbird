import { createAction } from '@tramvai/core';
import { getJson } from 'src/mockingbird/infrastructure/request';
import { setLoading, fetchSuccess, fetchFail, reset } from '../reducers';

export const fetchAction = createAction({
  name: 'FETCH_DESTINATIONS_ACTION',
  fn: ({ dispatch, getState }, service) => {
    dispatch(setLoading());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v3/destination`, { query: { service } })
      .then((response) => {
        if (!response || !Array.isArray(response)) {
          throw new Error();
        }
        return dispatch(fetchSuccess(response));
      })
      .catch(() => dispatch(fetchFail()));
  },
});

export const resetAction = createAction({
  name: 'RESET_DESTINATIONS_ACTION',
  fn: ({ dispatch }) => dispatch(reset()),
});
