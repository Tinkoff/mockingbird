import { createAction as createActionCore } from '@tramvai/core';
import { getJson } from 'src/mockingbird/infrastructure/request';
import {
  setLoading,
  createSuccess,
  createFail,
  reset,
} from '../reducers/createStore';


type ServiceRequestParams = {
  name: string,
  suffix: string
};


export const createAction = createActionCore({
  name: 'CREATE_SERVICE_ACTION',
  fn: ({ dispatch, getState }, body: ServiceRequestParams) => {
    dispatch(setLoading());
    const {
      environment: { MOCKINGBIRD_API },
    } = getState();
    return getJson(`${MOCKINGBIRD_API}/v2/service`, {
      httpMethod: 'post',
      body: normalizeRequest(body),
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


function normalizeRequest(params: ServiceRequestParams): ServiceRequestParams {
  return {
    name: params.name.trim(),
    suffix: params.suffix.trim()
  };
}


export const resetCreateStateAction = createActionCore({
  name: 'RESET_CREATE_SERVICE_ACTION',
  fn: ({ dispatch }) => dispatch(reset()),
});
