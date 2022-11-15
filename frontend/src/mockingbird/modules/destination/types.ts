export interface Destination {
  name: string;
  description: string;
  service: string;
  request: any;
  init?: any[];
  shutdown?: any[];
}

export interface DestinationFormData {
  name: string;
  description: string;
  request: string;
  init: string;
  shutdown: string;
}
