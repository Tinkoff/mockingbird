import { createAction as createActionCore } from '@tramvai/core';
import { PAGE_SERVICE_TOKEN } from '@tramvai/tokens-router';
import { getJson, patchJson } from 'src/mockingbird/infrastructure/request';
import {
  getSuccessToast,
  getCreateErrorToast,
  getUpdateErrorToast,
  getRemoveErrorToast,
} from 'src/infrastructure/notifications';
import { getPathSource } from 'src/mockingbird/paths';
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
  setDeleting,
  deleteFail,
  deleteSuccess,
  reset,
} from '../reducers';

export const createAction = createActionCore({
  name: 'CREATE_SOURCE_ACTION',
  fn: ({ dispatch, getState }, { data, serviceId }, { pageService }) => {
    dispatch(setCreating());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v3/source`, {
      httpMethod: 'post',
      body: data,
    })
      .then((response) => {
        if (response.status === 'success' && response.id) {
          dispatch(getSuccessToast('Источник успешно создан'));
          pageService.navigate(getPathSource(serviceId, response.id));
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
  name: 'FETCH_SOURCE_ACTION',
  fn: ({ dispatch, getState }, { name }) => {
    dispatch(setFetching());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v3/source/${name}`)
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
  name: 'UPDATE_SOURCE_ACTION',
  fn: ({ dispatch, getState }, { name, data }) => {
    dispatch(setUpdating());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return patchJson(`${MOCKINGBIRD_API}/v3/source/${name}`, { body: data })
      .then((response) => {
        if (response.status === 'success' && response.id) {
          dispatch(getSuccessToast('Источник успешно обновлен'));
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

export const deleteAction = createActionCore({
  name: 'DELETE_SOURCE_ACTION',
  fn: ({ dispatch, getState }, { name, basePath }, { pageService }) => {
    dispatch(setDeleting());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v3/source/${name}`, {
      httpMethod: 'delete',
    })
      .then((response) => {
        if (response.status === 'success') {
          dispatch(getSuccessToast('Источник успешно удален'));
          pageService.navigate(basePath);
          return dispatch(deleteSuccess());
        }
        dispatch(getRemoveErrorToast(null));
        return dispatch(deleteFail(null));
      })
      .catch((e) => {
        dispatch(getRemoveErrorToast(e));
        return dispatch(deleteFail(e));
      });
  },
  deps: {
    pageService: PAGE_SERVICE_TOKEN,
  },
});

export const resetAction = createActionCore({
  name: 'RESET_SOURCE_STATE_ACTION',
  fn: ({ dispatch }) => dispatch(reset()),
});
