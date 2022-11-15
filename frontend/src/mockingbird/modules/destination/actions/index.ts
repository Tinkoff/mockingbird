import { createAction as createActionCore } from '@tramvai/core';
import { PAGE_SERVICE_TOKEN } from '@tramvai/tokens-router';
import { getJson, patchJson } from 'src/mockingbird/infrastructure/request';
import {
  getSuccessToast,
  getCreateErrorToast,
  getUpdateErrorToast,
} from 'src/infrastructure/notifications';
import { getPathDestination } from 'src/mockingbird/paths';
import {
  setCreating,
  createSuccess,
  createFail,
  setFetching,
  fetchFail,
  fetchSuccess,
  setUpdating,
  updateFail,
  updateSuccess,
  reset,
} from '../reducers';

export const createAction = createActionCore({
  name: 'CREATE_DESTINATION_ACTION',
  fn: ({ dispatch, getState }, { data, serviceId }, { pageService }) => {
    dispatch(setCreating());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v3/destination`, {
      httpMethod: 'post',
      body: data,
    })
      .then((response) => {
        if (response.status === 'success' && response.id) {
          dispatch(getSuccessToast('Получатель успешно создан'));
          pageService.navigate(getPathDestination(serviceId, response.id));
          return dispatch(createSuccess());
        }
        throw new Error();
      })
      .catch((e) => {
        dispatch(getCreateErrorToast(e));
        return dispatch(createFail());
      });
  },
  deps: {
    pageService: PAGE_SERVICE_TOKEN,
  },
});

export const fetchAction = createActionCore({
  name: 'FETCH_DESTINATION_ACTION',
  fn: ({ dispatch, getState }, { name }) => {
    dispatch(setFetching());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v3/destination/${name}`)
      .then((response) => {
        if (!response || !response.name) {
          throw new Error();
        }
        return dispatch(fetchSuccess(response));
      })
      .catch(() => dispatch(fetchFail()));
  },
});

export const updateAction = createActionCore({
  name: 'UPDATE_DESTINATION_ACTION',
  fn: ({ dispatch, getState }, { name, data }) => {
    dispatch(setUpdating());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return patchJson(`${MOCKINGBIRD_API}/v3/destination/${name}`, {
      body: data,
    })
      .then((response) => {
        if (response.status === 'success' && response.id) {
          dispatch(getSuccessToast('Получатель успешно обновлен'));
          return dispatch(updateSuccess(data));
        }
        dispatch(getUpdateErrorToast(null));
        return dispatch(updateFail(null));
      })
      .catch((e) => {
        dispatch(getUpdateErrorToast(e));
        return dispatch(updateFail(e));
      });
  },
});

export const resetAction = createActionCore({
  name: 'RESET_DESTINATION_STATE_ACTION',
  fn: ({ dispatch }) => dispatch(reset()),
});
