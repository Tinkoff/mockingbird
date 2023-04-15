import { useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import store from '../reducers';
import { fetchAction } from '../actions';
import type { Source } from '../types';

export function useSources(serviceId: string) {
  const { status, sources } = useStoreSelector(store, selectorAsIs);
  const fetchSources = useActions(fetchAction);
  useEffect(() => {
    if (status === 'none') {
      fetchSources(serviceId);
    }
  }, [fetchSources, serviceId, status]);
  return sources.map(mapSelectItem);
}

export function mapSelectItem({ name: value, description }: Source) {
  return {
    label: value,
    description,
    value,
  };
}
