import { useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import store from '../reducers/store';
import { fetchAction } from '../actions/fetchAction';

export default function useService(serviceId: string) {
  const { data: service } = useStoreSelector(store, selectorAsIs);
  const fetchService = useActions(fetchAction);
  useEffect(() => {
    if (!process.env.BROWSER) return;
    fetchService(serviceId);
  }, [serviceId, fetchService]);
  return service;
}
