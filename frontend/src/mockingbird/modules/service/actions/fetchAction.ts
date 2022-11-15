import { createAction } from '@tramvai/core';
import { getJson } from 'src/mockingbird/infrastructure/request';
import { setLoading, fetchSuccess, fetchFail } from '../reducers/store';

export const fetchAction = createAction({
  name: 'FETCH_SERVICE_ACTION',
  fn: ({ dispatch, getState }, id) => {
    dispatch(setLoading());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v2/service/${id}`)
      .then((response) => {
        if (!response || !response.name) {
          throw new Error();
        }
        return dispatch(fetchSuccess(response));
      })
      .catch(() => dispatch(fetchFail()));
  },
});
