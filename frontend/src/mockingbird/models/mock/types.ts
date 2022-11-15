export type Mock = THTTPMock | TScenarioMock | TGRPCMock;

export type THTTPMock = {
  id: string;
  name: string;
  method: string;
  path?: string;
  pathPattern?: string;
  scope: string;
  times: number;
  labels: string[];
  request: any;
  response: any;
  state: any;
  persist: any;
  seed: any;
  callback?: TCallBack;
};

export type TScenarioMock = {
  id: string;
  name: string;
  source: string;
  destination: string;
  scope: string;
  times?: number;
  labels: string[];
  input: any;
  output: any;
  state: any;
  persist: any;
  seed: any;
  callback?: TCallBack;
};

export type TGRPCMock = {
  id: string;
  name: string;
  scope: string;
  times?: number;
  labels: string[];
  methodName: string;
  requestCodecs: any;
  requestSchema?: any;
  requestClass: string;
  requestPredicates: any;
  responseCodecs: any;
  responseSchema?: any;
  responseClass: string;
  response: any;
  state: any;
  seed: any;
};

export type TCallBack = TCallBackHTTP | TCallbackMessage;

export type TCallBackHTTP = {
  type: 'http';
  request: any;
  responseMode?: 'json' | 'xml';
  persist?: any;
  delay?: string;
  callback?: TCallBack;
};

export type TCallbackMessage = {
  type: 'message';
  destination: string;
  output: any;
  delay?: string;
  callback?: TCallBack;
};
