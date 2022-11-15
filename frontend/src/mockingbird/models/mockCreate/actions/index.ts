import { createAction as createActionCore } from '@tramvai/core';
import { PAGE_SERVICE_TOKEN } from '@tramvai/tokens-router';
import { getJson } from 'src/mockingbird/infrastructure/request';
import {
  getSuccessToast,
  getCreateErrorToast,
} from 'src/infrastructure/notifications';
import { getPathMock } from 'src/mockingbird/paths';
import {
  setLoading,
  createSuccess,
  createFail,
  reset,
} from '../reducers/store';

export const createAction = createActionCore({
  name: 'CREATE_MOCK_ACTION',
  fn: ({ dispatch, getState }, { data, serviceId, type }, { pageService }) => {
    dispatch(setLoading());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    const method = getMethodByType(type);
    return getJson(`${MOCKINGBIRD_API}/v2/${method}`, {
      httpMethod: 'post',
      body: data,
    })
      .then((response) => {
        if (response.status === 'success' && response.id) {
          dispatch(getSuccessToast(getSuccessMessageByType(type)));
          pageService.navigate(getPathMock(serviceId, response.id, type));
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

export const resetCreateStateAction = createActionCore({
  name: 'RESET_CREATE_MOCK_ACTION',
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

function getSuccessMessageByType(method: string) {
  switch (method) {
    case 'http':
      return 'Стаб успешно создан';
    case 'scenario':
      return 'Сценарий успешно создан';
    case 'grpc':
      return 'GRPC успешно создан';
    default:
      return '';
  }
}
