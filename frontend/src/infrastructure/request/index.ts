import request from '@tinkoff/request-core';
import deduplicateCache from '@tinkoff/request-plugin-cache-deduplicate';
import memoryCache from '@tinkoff/request-plugin-cache-memory';
import http from '@tinkoff/request-plugin-protocol-http';
import memoryConstructor from '../utils/lruCache';

const makeRequest = request([
  memoryCache({
    memoryConstructor,
    lruOptions: { max: 1000, maxAge: 15 * 60 * 1000 },
  }),
  deduplicateCache(),
  http(),
]);

export function getJson(url: string, abortPromise?: Promise<any>) {
  return makeRequest({
    url,
    httpMethod: 'get',
    cache: true,
    abortPromise,
  });
}
