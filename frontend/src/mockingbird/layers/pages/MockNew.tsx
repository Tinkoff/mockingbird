import React, { useState, useCallback } from 'react';
import { useUrl } from '@tramvai/module-router';
import { SegmentedControl } from '@mantine/core';
import PageHeader from 'src/components/PageHeader/PageHeader';
import useLabels from 'src/mockingbird/modules/labels';
import { getPathMocks } from 'src/mockingbird/paths';
import HttpNew from './mock/HttpNew';
import ScenarioNew from './mock/ScenarioNew';
import GrpcNew from './mock/GrpcNew';
import styles from './common.css';

const TYPES = [
  {
    label: 'HTTP',
    value: 'HTTP',
  },
  {
    label: 'Scenario',
    value: 'Scenario',
  },
  {
    label: 'GRPC',
    value: 'GRPC',
  },
];

export default function MockNew() {
  const url = useUrl();
  const serviceId = Array.isArray(url.query.service)
    ? url.query.service[0]
    : url.query.service;
  const labels = useLabels(serviceId);

  const [type, setType] = useState(TYPES[0].value);
  const onChangeType = useCallback(
    (value) => {
      setType(value);
    },
    [setType]
  );

  return (
    <div className={styles.root}>
      <PageHeader
        title="Создание мока"
        backText="К списку моков"
        backPath={getPathMocks(serviceId)}
        right={
          <SegmentedControl
            size="m"
            value={type}
            data={TYPES}
            onChange={onChangeType}
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
