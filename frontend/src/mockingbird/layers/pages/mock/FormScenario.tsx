import React, { useCallback, useState } from 'react';
import { useForm } from 'react-hook-form';
import {
  Box,
  Button,
  Flex,
  Space,
  Title,
  Accordion,
  Group,
} from '@mantine/core';
import { Input } from 'src/mockingbird/components/form/Input';
import InputCount from 'src/mockingbird/components/form/InputCount';
import InputSearchTagged from 'src/mockingbird/components/form/InputSearchTagged';
import Select from 'src/mockingbird/components/form/Select';
import Sources from 'src/mockingbird/modules/sources';
import Destinations from 'src/mockingbird/modules/destinations';
import { InputJson } from 'src/mockingbird/components/form/InputJson';
import type { TScenarioMock } from 'src/mockingbird/models/mock/types';
import Callbacks from './Callbacks';
import JSONRequest from './JSONRequest';
import { mapScenarioToFormData, mapFormDataToScenario } from './utils';
import { SCOPES } from './refs';
import type { TScenarioFormData, TFormCallback } from './types';

type Props = {
  labels: string[];
  serviceId: string;
  data?: TScenarioMock;
  actions?: React.ReactElement;
  submitText?: string;
  submitDisabled?: boolean;
  onSubmit: (data: TScenarioFormData, callbacks: TFormCallback[]) => void;
};

export default function FormScenario(props: Props) {
  const {
    labels,
    serviceId,
    data,
    actions,
    submitText = 'Создать',
    submitDisabled = false,
    onSubmit: onSubmitParent,
  } = props;
  const { callbacks: defaultCallbacks, ...defaultValues } =
    mapScenarioToFormData(data);
  const [callbacks, setCallbacks] = useState<TFormCallback[]>(defaultCallbacks);
  const { control, watch, handleSubmit } = useForm<TScenarioFormData>({
    defaultValues,
    mode: 'onBlur',
  });
  const scope = watch('scope');
  const onSubmit = useCallback(
    (formData: TScenarioFormData) => onSubmitParent(formData, callbacks),
    [callbacks, onSubmitParent]
  );
  const onGetValues = useCallback(() => {
    return mapFormDataToScenario(watch(), serviceId, callbacks);
  }, [watch, callbacks, serviceId]);
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input
        name="name"
        label="Название"
        control={control as any}
        required
        mb="sm"
      />
      <InputSearchTagged
        name="labels"
        label="Лейблы"
        options={labels}
        control={control as any}
        mb="sm"
      />
      <Flex mb="sm">
        <Box sx={{ width: '50%' }}>
          <Select
            name="scope"
            label="Время жизни"
            options={SCOPES}
            control={control as any}
            required
          />
        </Box>
        <Space w="md" />
        {scope === 'countdown' ? (
          <Box sx={{ width: '50%' }}>
            <InputCount
              name="times"
              label="Количество срабатываний"
              min={1}
              control={control as any}
            />
          </Box>
        ) : (
          <Box sx={{ width: '50%' }} />
        )}
      </Flex>
      <Title order={4}>Источник</Title>
      <Sources
        name="source"
        label="Источник событий"
        serviceId={serviceId}
        control={control as any}
        required
        mb="sm"
      />
      <InputJson
        name="input"
        label="Запрос"
        control={control as any}
        required
        mb="sm"
      />
      <InputJson
        name="state"
        label="Предикат для поиска состояния"
        control={control as any}
        mb="sm"
      />
      <Title order={4}>Получатель</Title>
      <Destinations
        name="destination"
        label="Получатель событий"
        serviceId={serviceId}
        control={control as any}
        mb="sm"
      />
      <InputJson
        name="output"
        label="Ответ"
        control={control as any}
        required
        mb="sm"
      />
      <InputJson
        name="persist"
        label="Данные, записываемые в базу"
        control={control as any}
        mb="sm"
      />
      <InputJson
        name="seed"
        label="Генерация переменных"
        control={control as any}
        mb="sm"
      />
      <Callbacks
        serviceId={serviceId}
        callbacks={callbacks}
        onChange={setCallbacks}
        mb="md"
      />
      <JSONRequest getValues={onGetValues} mb="md" />
      {actions && (
        <Accordion variant="contained" mb="md">
          <Accordion.Item value="actions">
            <Accordion.Control>Действия</Accordion.Control>
            <Accordion.Panel>{actions}</Accordion.Panel>
          </Accordion.Item>
        </Accordion>
      )}
      <Group position="right">
        <Button type="submit" size="md" disabled={submitDisabled}>
          {submitText}
        </Button>
      </Group>
    </form>
  );
}
