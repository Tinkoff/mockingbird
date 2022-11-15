import { createAction } from '@tramvai/core';
import { getJson } from 'src/mockingbird/infrastructure/request';
import { setLoading, fetchSuccess, fetchFail } from '../reducers';

export const fetchAction = createAction({
  name: 'FETCH_LABELS_ACTION',
  fn: ({ dispatch, getState }, service) => {
    dispatch(setLoading());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v2/label`, { query: { service } })
      .then((response) => {
        if (!response || !Array.isArray(response)) {
          throw new Error();
        }
        return dispatch(fetchSuccess(response));
      })
      .catch(() => dispatch(fetchFail()));
  },
});
