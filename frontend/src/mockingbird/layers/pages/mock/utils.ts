import { v4 as generateId } from 'uuid';
import {
  parseJSON,
  stringifyJSON,
} from 'src/mockingbird/infrastructure/utils/forms';
import type {
  THTTPMock,
  TScenarioMock,
  TGRPCMock,
  TCallBack,
  TCallBackHTTP,
} from 'src/mockingbird/models/mock/types';
import {
  SCOPES,
  METHODS,
  DEFAULT_REQUEST,
  DEFAULT_RESPONSE,
  DEFAULT_SCENARIO_INPUT,
} from './refs';
import type {
  THTTPFormData,
  TScenarioFormData,
  TGRPCFormData,
  TFormCallback,
} from './types';

export function mapStubToFormData(serviceId: string, data?: THTTPMock) {
  if (!data)
    return {
      name: 'Стаб ***',
      labels: [],
      scope: SCOPES[0].value,
      times: 1,
      method: METHODS[0].value,
      path: '/',
      isPathPattern: false,
      request: stringifyJSON(DEFAULT_REQUEST),
      response: stringifyJSON(DEFAULT_RESPONSE),
      state: stringifyJSON({}),
      persist: stringifyJSON({}),
      seed: stringifyJSON({}),
      callbacks: [],
    };
  return {
    name: data.name,
    labels: data.labels,
    scope: data.scope,
    times: data.times,
    method: data.method,
    path: (data.pathPattern || data.path || '').replace(`/${serviceId}`, ''),
    isPathPattern: Boolean(data.pathPattern),
    request: stringifyJSON(data.request),
    response: stringifyJSON(data.response),
    state: stringifyJSON(data.state),
    persist: stringifyJSON(data.persist),
    seed: stringifyJSON(data.seed),
    callbacks: mapCallbackToFormCallbacks(data.callback),
  };
}

export function mapFormDataToStub(
  data: THTTPFormData,
  serviceId: string,
  callbacks: TFormCallback[]
) {
  const { name, labels, scope, times, method, path, isPathPattern } = data;
  return {
    ...mapPaths(path, isPathPattern, serviceId),
    name,
    labels,
    method,
    scope,
    times: scope === 'countdown' ? times : undefined,
    request: parseJSON(data.request),
    response: parseJSON(data.response),
    state: parseJSON(data.state, true), // бэк ругается на пустой объект
    persist: parseJSON(data.persist, true), // не ругается, но бэк советует также null
    seed: parseJSON(data.seed, true),
    callback: mapFormCallbacksToCallback(callbacks),
  };
}

export function mapScenarioToFormData(data?: TScenarioMock) {
  if (!data)
    return {
      name: 'Сценарий ***',
      labels: [],
      scope: SCOPES[0].value,
      times: 1,
      source: '',
      destination: '',
      input: stringifyJSON(DEFAULT_SCENARIO_INPUT),
      output: stringifyJSON({}),
      state: stringifyJSON({}),
      persist: stringifyJSON({}),
      seed: stringifyJSON({}),
      callbacks: [],
    };
  return {
    name: data.name,
    labels: data.labels,
    scope: data.scope,
    times: data.times,
    source: data.source,
    destination: data.destination,
    input: stringifyJSON(data.input),
    output: stringifyJSON(data.output),
    state: stringifyJSON(data.state),
    persist: stringifyJSON(data.persist),
    seed: stringifyJSON(data.seed),
    callbacks: mapCallbackToFormCallbacks(data.callback),
  };
}

export function mapFormDataToScenario(
  data: TScenarioFormData,
  serviceId: string,
  callbacks: TFormCallback[]
) {
  const { name, labels, scope, times, source, destination } = data;
  return {
    name,
    labels,
    scope,
    times: scope === 'countdown' ? times : undefined,
    source,
    destination: destination || null,
    input: parseJSON(data.input),
    output: parseJSON(data.output, true),
    state: parseJSON(data.state, true), // бэк ругается на пустой объект
    persist: parseJSON(data.persist, true), // не ругается, но бэк советует также null
    seed: parseJSON(data.seed, true),
    callback: mapFormCallbacksToCallback(callbacks),
    service: serviceId,
  };
}

export function mapGrpcToFormData(serviceId: string, data?: TGRPCMock) {
  if (!data)
    return {
      name: 'GRPC ***',
      labels: [],
      scope: SCOPES[0].value,
      times: 1,
      methodName: 'ReferenceService/Search' /* test, then empty str */,
      requestClass: 'CarSearchRequest' /* test */,
      /* eslint-disable-next-line @typescript-eslint/naming-convention */
      requestPredicates: stringifyJSON({ year: { '==': 2019 } }) /* test */,
      responseClass: 'CarSearchResponse' /* test */,
      response: stringifyJSON({
        /* test */ mode: 'fill',
        data: {},
        // delay: "20 seconds"
      }),
      state: stringifyJSON({}),
      seed: stringifyJSON({}),
    };
  return {
    name: data.name,
    labels: data.labels,
    scope: data.scope,
    times: data.times,
    methodName: data.methodName,
    requestCodecs: data.requestCodecs,
    requestSchema: stringifyJSON(data.requestSchema),
    requestClass: data.requestClass,
    requestPredicates: stringifyJSON(data.requestPredicates),
    responseCodecs: data.responseCodecs,
    responseSchema: stringifyJSON(data.responseSchema),
    responseClass: data.responseClass,
    response: stringifyJSON(data.response),
    state: stringifyJSON(data.state),
    seed: stringifyJSON(data.seed),
  };
}

export function mapFormDataToGrpc(data: TGRPCFormData, serviceId: string) {
  const {
    name,
    labels,
    scope,
    times,
    methodName,
    requestClass,
    responseClass,
  } = data;
  const promises: any = [
    Promise.resolve({
      name,
      labels,
      scope,
      times: scope === 'countdown' ? times : undefined,
      methodName,
      requestClass,
      requestPredicates: parseJSON(data.requestPredicates),
      responseClass,
      response: parseJSON(data.response),
      state: parseJSON(data.state, true),
      seed: parseJSON(data.seed, true),
      service: serviceId,
    }),
  ];
  if (data.requestCodecs && data.requestCodecs.length)
    promises.push(
      fileToBase64String(data.requestCodecs[0]).then((requestCodecs) => ({
        requestCodecs,
      }))
    );
  if (data.responseCodecs && data.responseCodecs.length)
    promises.push(
      fileToBase64String(data.responseCodecs[0]).then((responseCodecs) => ({
        responseCodecs,
      }))
    );
  return Promise.all(promises).then((results) => Object.assign({}, ...results));
}

function mapPaths(_path: string, isPattern: boolean, serviceId: string) {
  let path = null;
  let pathPattern = null;
  const result = `/${serviceId}${_path}`;
  if (isPattern) pathPattern = result;
  else path = result;
  return {
    path,
    pathPattern,
  };
}

function mapCallbackToFormCallbacks(callback?: TCallBack): TFormCallback[] {
  if (!callback) return [];
  let c = callback;
  const callbacks = [];
  while (c) {
    callbacks.push(mapCallback(c));
    if (c.callback) {
      c = c.callback;
    } else {
      break;
    }
  }
  return callbacks;
}

function mapFormCallbacksToCallback(
  callbacks: TFormCallback[]
): TCallBack | undefined {
  if (!callbacks.length) return;
  const callback: TCallBack = mapFormCallback(callbacks[0]);
  let c = callback;
  for (let i = 1; i < callbacks.length; i++) {
    c.callback = mapFormCallback(callbacks[i]);
    c = c.callback;
  }
  return callback;
}

function mapCallback(callback: TCallBack): TFormCallback {
  if (callback.type === 'http') {
    return {
      id: generateId(),
      type: callback.type,
      request: stringifyJSON(callback.request),
      responseMode: callback.responseMode || '',
      persist: stringifyJSON(callback.persist),
      delay: callback.delay,
    };
  }
  if (callback.type === 'message') {
    return {
      id: generateId(),
      type: callback.type,
      destination: callback.destination,
      output: stringifyJSON(callback.output),
      delay: callback.delay,
    };
  }
  throw new Error('Missed callback type while mapping');
}

function mapFormCallback(callback: TFormCallback): TCallBack {
  if (callback.type === 'http') {
    const res: TCallBackHTTP = {
      type: callback.type,
      request: parseJSON(callback.request),
      delay: callback.delay || undefined,
    };
    if (callback.responseMode) {
      res.responseMode = callback.responseMode;
      res.persist = parseJSON(callback.persist);
    }
    return res;
  }
  if (callback.type === 'message') {
    return {
      type: callback.type,
      destination: callback.destination,
      output: parseJSON(callback.output),
      delay: callback.delay || undefined,
    };
  }
  throw new Error('Missing callback type while mapping');
}

function fileToBase64String(file: File) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => {
      const res = reader.result;
      resolve(
        typeof res === 'string'
          ? res.replace('data:', '').replace(/^.+,/, '')
          : res
      );
    };
    reader.onerror = (error) => reject(error);
  });
}
