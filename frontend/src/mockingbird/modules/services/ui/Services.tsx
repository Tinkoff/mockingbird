import React, { useState, useEffect, useCallback } from 'react';
import { connect, useActions, useStoreSelector } from '@tramvai/state';
import Button from '@platform-ui/button';
import Loader from '@platform-ui/loader';
import PageHeader from 'src/components/PageHeader/PageHeader';
import List from 'src/components/List/List';
import ListError from 'src/components/List/ListError';
import ListEmpty from 'src/components/List/ListEmpty';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import Page from 'src/mockingbird/components/Page';
import servicesStore from '../reducers/store';
import { fetchAction } from '../actions/fetchAction';
import ServiceItem from './ServiceItem';
import CreatePopup from './CreatePopup';

interface Props {
  execApiPath: string;
}

function Services({ execApiPath }: Props) {
  const fetchServices = useActions(fetchAction);
  const { services, status } = useStoreSelector(servicesStore, selectorAsIs);
  const [creating, setCreating] = useState(false);
  useEffect(() => {
    if (!process.env.BROWSER) return;
    fetchServices();
  }, [fetchServices]);
  const handleCreate = useCallback(() => {
    setCreating(true);
  }, []);
  const handleCreateEnd = useCallback(() => {
    setCreating(false);
  }, []);
  return (
    <Page>
      <PageHeader
        title="Сервисы"
        right={
          <Button size="m" disabled={creating} onClick={handleCreate}>
            Создать
          </Button>
        }
      />
      {status === 'loading' && <Loader size="xxl" centered />}
      {status === 'error' && <ListError onRetry={fetchServices} />}
      {status === 'complete' && !services.length && <ListEmpty />}
      {services.length > 0 && (
        <List>
          {services.map((i) => (
            <ServiceItem key={i.name} item={i} execApiPath={execApiPath} />
          ))}
        </List>
      )}
      {creating && <CreatePopup opened={creating} onClose={handleCreateEnd} />}
    </Page>
  );
}

const mapProps = ({ environment: { MOCKINGBIRD_EXEC_API: execApiPath } }) => ({
  execApiPath,
});

export default connect([], mapProps)(Services);
