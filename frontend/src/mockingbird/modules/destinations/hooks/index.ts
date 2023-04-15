import { useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import store from '../reducers';
import { fetchAction } from '../actions';
import type { Destination } from '../types';

export function useDestinations(serviceId: string) {
  const { status, destinations } = useStoreSelector(store, selectorAsIs);
  const fetchDestinations = useActions(fetchAction);
  useEffect(() => {
    if (status === 'none') {
      fetchDestinations(serviceId);
    }
  }, [fetchDestinations, serviceId, status]);
  return destinations.map(mapSelectItem);
}

export function mapSelectItem({ name: value, description }: Destination) {
  return {
    label: value,
    description,
    value,
  };
}
