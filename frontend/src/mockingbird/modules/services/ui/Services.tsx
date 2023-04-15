import React, { useEffect } from 'react';
import { connect, useActions, useStoreSelector } from '@tramvai/state';
import { Button } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import PageHeader from 'src/components/PageHeader/PageHeader';
import List from 'src/components/List/List';
import ListError from 'src/components/List/ListError';
import ListEmpty from 'src/components/List/ListEmpty';
import { ListLoading } from 'src/components/List/ListLoading';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import type { ServicesState } from '../reducers/store';
import servicesStore from '../reducers/store';
import { fetchAction } from '../actions/fetchAction';
import ServiceItem from './ServiceItem';
import CreatePopup from './CreatePopup';

type Props = {
  execApiPath: string;
};

const SERVICE_ITEM_HEIGHT = 80;

function Services({ execApiPath }: Props) {
  const fetchServices = useActions(fetchAction);
  const { services, status } = useStoreSelector(
    servicesStore,
    selectorAsIs as (s: ServicesState) => ServicesState
  );
  useEffect(() => {
    if (!process.env.BROWSER) return;
    fetchServices();
  }, [fetchServices]);
  const [creating, { open: handleCreate, close: handleCreateEnd }] =
    useDisclosure(false);
  return (
    <>
      <PageHeader
        title="Сервисы"
        right={
          <Button
            size="sm"
            disabled={status === 'loading'}
            onClick={handleCreate}
          >
            Создать
          </Button>
        }
      />
      {status === 'loading' && <ListLoading mih={SERVICE_ITEM_HEIGHT} />}
      {status === 'error' && <ListError onRetry={fetchServices} />}
      {status === 'complete' && !services.length && <ListEmpty />}
      {status === 'complete' && services.length > 0 && (
        <List>
          {services.map((i) => (
            <ServiceItem key={i.name} item={i} execApiPath={execApiPath} />
          ))}
        </List>
      )}
      {creating && <CreatePopup opened={creating} onClose={handleCreateEnd} />}
    </>
  );
}

const mapProps = ({ environment: { MOCKINGBIRD_EXEC_API: execApiPath } }) => ({
  execApiPath,
});

export default connect([], mapProps)(Services as any);
