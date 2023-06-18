import {
  parseJSON,
  stringifyJSON,
} from 'src/mockingbird/infrastructure/utils/forms';
import { DEFAULT_REQUEST } from './refs';
import type { Source, SourceFormData } from './types';

export function mapSourceToFormData(data?: Source): SourceFormData {
  if (!data)
    return {
      name: 'test_in',
      description: 'Источник ***',
      request: stringifyJSON(DEFAULT_REQUEST),
      init: stringifyJSON([]),
      shutdown: stringifyJSON([]),
      reInitTriggers: stringifyJSON([]),
    };
  return {
    name: data.name,
    description: data.description,
    request: stringifyJSON(data.request),
    init: stringifyJSON(data.init, []),
    shutdown: stringifyJSON(data.shutdown, []),
    reInitTriggers: stringifyJSON(data.reInitTriggers, []),
  };
}

export function mapFormDataToSource(
  data: SourceFormData,
  serviceId: string
): Source {
  const { name, description } = data;
  return {
    name: name.trim(),
    description: description.trim(),
    service: serviceId,
    request: parseJSON(data.request),
    init: parseJSON(data.init, true),
    shutdown: parseJSON(data.shutdown, true),
    reInitTriggers: parseJSON(data.reInitTriggers, true),
  };
}
