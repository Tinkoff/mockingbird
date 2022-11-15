export type THTTPFormData = {
  name: string;
  labels: string[];
  scope: string;
  times: number;
  method: string;
  path: string;
  isPathPattern: boolean;
  request: string;
  response: string;
  state: string;
  persist: string;
  seed: string;
};

export type TScenarioFormData = {
  name: string;
  labels: string[];
  scope: string;
  times: number;
  source: string;
  destination: string;
  input: string;
  output: string;
  state: string;
  persist: string;
  seed: string;
};

export type TGRPCFormData = {
  name: string;
  labels: string[];
  scope: string;
  times: number;
  methodName: string;
  requestCodecs: any;
  requestSchema?: any;
  requestClass: string;
  requestPredicates: string;
  responseCodecs: any;
  responseSchema?: any;
  responseClass: string;
  response: string;
  state: string;
  seed: string;
};

export type TFormCallback = TFormCallbackHTTP | TFormCallbackMessage;

export type TFormCallbackHTTP = {
  type: 'http';
  request: string;
  responseMode: '' | 'json' | 'xml';
  persist: string;
  delay?: string;
  id: string;
};

export type TFormCallbackMessage = {
  type: 'message';
  destination: string;
  output: string;
  delay?: string;
  id: string;
};
