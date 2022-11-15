import { mapSelectItem } from 'src/mockingbird/infrastructure/helpers/forms';

export const SCOPES = [
  {
    title: 'Вечный',
    value: 'persistent',
  },
  {
    title: 'Эфемерный',
    value: 'ephemeral',
  },
  {
    title: 'N-разовый',
    value: 'countdown',
  },
];

export const METHODS = [
  'POST',
  'GET',
  'PATCH',
  'DELETE',
  'PUT',
  'HEAD',
  'OPTIONS',
].map(mapSelectItem);

export const DEFAULT_REQUEST = {
  mode: 'json',
  body: {},
  headers: {
    /* eslint-disable-next-line @typescript-eslint/naming-convention */
    'Content-Type': 'application/json',
  },
};
export const DEFAULT_RESPONSE = {
  mode: 'json',
  body: {},
  headers: {
    /* eslint-disable-next-line @typescript-eslint/naming-convention */
    'Content-Type': 'application/json',
  },
  code: '200',
};

export const DEFAULT_SCENARIO_INPUT = {
  mode: 'json',
  payload: {},
};

export const CALLBACK_TYPES = [
  {
    title: 'HTTP',
    value: 'http',
  },
  {
    title: 'Message',
    value: 'message',
  },
];

export const CALLBACK_HTTP_RESPONSE_TYPES = [
  {
    title: '-',
    value: '',
  },
  {
    title: 'JSON',
    value: 'json',
  },
  {
    title: 'XML',
    value: 'xml',
  },
];

const DEFAULT_CALLBACK_HTTP_REQUEST = {
  url: 'https://tinkoff.ru',
  method: 'POST',
  mode: 'no_body',
  headers: {},
};

const DEFAULT_CALLBACK_MESSAGE_OUTPUT = {
  mode: 'json',
  payload: {},
};

export const DEFAULT_CALLBACK_HTTP = {
  type: 'http',
  request: JSON.stringify(DEFAULT_CALLBACK_HTTP_REQUEST, undefined, 2),
  responseMode: '',
  persist: JSON.stringify({}),
};

export const DEFAULT_CALLBACK_MESSAGE = {
  type: 'message',
  destination: '',
  output: JSON.stringify(DEFAULT_CALLBACK_MESSAGE_OUTPUT, undefined, 2),
};
