import React, { useState, useCallback } from 'react';
import { useUrl } from '@tramvai/module-router';
import { TabsBlockInline } from '@platform-ui/tabsBlock';
import PageHeader from 'src/components/PageHeader/PageHeader';
import useLabels from 'src/mockingbird/modules/labels';
import { getPathMocks } from 'src/mockingbird/paths';
import HttpNew from './mock/HttpNew';
import ScenarioNew from './mock/ScenarioNew';
import GrpcNew from './mock/GrpcNew';
import styles from './common.css';

const TYPES = [
  {
    title: 'HTTP',
  },
  {
    title: 'Scenario',
  },
  {
    title: 'GRPC',
  },
];

export default function MockNew() {
  const {
    query: { service: serviceId },
  } = useUrl();
  const labels = useLabels(serviceId);

  const [type, setType] = useState(TYPES[0].title);
  const onChangeType = useCallback((_, { title }) => {
    setType(title);
  }, []);

  return (
    <div className={styles.root}>
      <PageHeader
        title="Создание мока"
        backText="К списку моков"
        backPath={getPathMocks(serviceId)}
        right={
          <TabsBlockInline
            size="m"
            activeIndex={TYPES.findIndex((i) => i.title === type)}
            items={TYPES}
            onItemClick={onChangeType}
          />
        }
      />
      {type === 'HTTP' && <HttpNew labels={labels} serviceId={serviceId} />}
      {type === 'Scenario' && (
        <ScenarioNew labels={labels} serviceId={serviceId} />
      )}
      {type === 'GRPC' && <GrpcNew labels={labels} serviceId={serviceId} />}
    </div>
  );
}
