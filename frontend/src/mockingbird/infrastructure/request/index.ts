import request from '@tinkoff/request-core';
import http from '@tinkoff/request-plugin-protocol-http';

const EMPTY_OBJECT = {};

const makeRequest = request([http()]);

export function getJson(url: string, options: any = EMPTY_OBJECT) {
  const {
    body,
    query,
    httpMethod = 'get',
    type = 'application/json',
  } = options;
  return makeRequest({
    ...options,
    url,
    payload: body ? JSON.stringify(body) : body,
    query,
    httpMethod,
    type,
  });
}

export function patchJson(url: string, options: any = EMPTY_OBJECT) {
  return getJson(url, { ...options, httpMethod: 'patch' });
}
