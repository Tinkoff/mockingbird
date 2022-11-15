import { createAction } from '@tramvai/core';
import { PAGE_SERVICE_TOKEN } from '@tramvai/tokens-router';
import { getJson, patchJson } from 'src/mockingbird/infrastructure/request';
import {
  getSuccessToast,
  getUpdateErrorToast,
  getRemoveErrorToast,
} from 'src/infrastructure/notifications';
import {
  setLoading,
  fetchSuccess,
  fetchFail,
  setUpdating,
  updateSuccess,
  updateFail,
  setDeleting,
  deleteSuccess,
  deleteFail,
  reset,
} from '../reducers/store';

export const fetchAction = createAction({
  name: 'FETCH_MOCK_ACTION',
  fn: ({ dispatch, getState }, { id, type }) => {
    dispatch(setLoading());
    const method = getMethodByType(type);
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v2/${method}/${id}`)
      .then((response) => {
        if (!response || !response.id) {
          throw new Error();
        }
        return dispatch(fetchSuccess(response));
      })
      .catch(() => dispatch(fetchFail()));
  },
});

export const updateAction = createAction({
  name: 'UPDATE_MOCK_ACTION',
  fn: ({ dispatch, getState }, { id, type, data }) => {
    dispatch(setUpdating());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    const method = getMethodByType(type);
    return patchJson(`${MOCKINGBIRD_API}/v2/${method}/${id}`, { body: data })
      .then((response) => {
        if (response.status === 'success' && response.id) {
          dispatch(getSuccessToast('Мок успешно обновлен'));
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

export const deleteAction = createAction({
  name: 'DELETE_MOCK_ACTION',
  fn: ({ dispatch, getState }, { id, type, basePath }, { pageService }) => {
    dispatch(setDeleting());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    const method = getMethodByType(type);
    return getJson(`${MOCKINGBIRD_API}/v2/${method}/${id}`, {
      httpMethod: 'delete',
    })
      .then((response) => {
        if (response.status === 'success') {
          dispatch(getSuccessToast('Мок успешно удален'));
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

export const resetMockStateAction = createAction({
  name: 'RESET_MOCK_STATE_ACTION',
  fn: ({ dispatch }) => dispatch(reset()),
});

function getMethodByType(method: string) {
  switch (method) {
    case 'http':
      return 'stub';
    case 'scenario':
      return 'scenario';
    case 'grpc':
      return 'grpcStub';
    default:
      return '';
  }
}
