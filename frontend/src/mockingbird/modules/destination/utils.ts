import {
  parseJSON,
  stringifyJSON,
} from 'src/mockingbird/infrastructure/utils/forms';
import { DEFAULT_REQUEST } from './refs';
import type { Destination, DestinationFormData } from './types';

export function mapDestinationToFormData(
  data?: Destination
): DestinationFormData {
  if (!data)
    return {
      name: 'test_out',
      description: 'Получатель ***',
      request: stringifyJSON(DEFAULT_REQUEST),
      init: stringifyJSON([]),
      shutdown: stringifyJSON([]),
    };
  return {
    name: data.name,
    description: data.description,
    request: stringifyJSON(data.request),
    init: stringifyJSON(data.init, []),
    shutdown: stringifyJSON(data.shutdown, []),
  };
}

export function mapFormDataToDestination(
  data: DestinationFormData,
  serviceId: string
): Destination {
  const { name, description } = data;
  return {
    name: name.trim(),
    description: description.trim(),
    service: serviceId,
    request: parseJSON(data.request),
    init: parseJSON(data.init, true),
    shutdown: parseJSON(data.shutdown, true),
  };
}
