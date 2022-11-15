import { createAction } from '@tramvai/core';
import { getJson } from 'src/mockingbird/infrastructure/request';
import { setLoading, fetchSuccess, fetchFail } from '../reducers/store';

export const fetchAction = createAction({
  name: 'FETCH_SERVICES_ACTION',
  fn: ({ dispatch, getState }) => {
    dispatch(setLoading());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v2/service`)
      .then((response) => {
        if (!response || !Array.isArray(response)) {
          throw new Error();
        }
        return dispatch(fetchSuccess(response));
      })
      .catch(() => dispatch(fetchFail()));
  },
});
