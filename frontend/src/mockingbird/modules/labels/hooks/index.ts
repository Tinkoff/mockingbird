import { useEffect } from 'react';
import { useActions, useStoreSelector } from '@tramvai/state';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import labelsStore from '../reducers';
import { fetchAction as fetchLabelsAction } from '../actions';

export default function useLabels(serviceId: string): string[] {
  const { labels } = useStoreSelector(labelsStore, selectorAsIs);
  const fetchLabels = useActions(fetchLabelsAction);
  useEffect(() => {
    if (!process.env.BROWSER) return;
    fetchLabels(serviceId);
  }, [serviceId, fetchLabels]);
  return labels;
}
