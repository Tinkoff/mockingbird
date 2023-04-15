import type { ReactNode } from 'react';
import React, { useCallback } from 'react';
import { useForm } from 'react-hook-form';
import {
  Box,
  Flex,
  Space,
  Title,
  Button,
  Textarea,
  Accordion,
  Group,
} from '@mantine/core';
import AttachFile from 'src/mockingbird/components/form/AttachFile';
import { Input } from 'src/mockingbird/components/form/Input';
import InputCount from 'src/mockingbird/components/form/InputCount';
import InputSearchTagged from 'src/mockingbird/components/form/InputSearchTagged';
import Select from 'src/mockingbird/components/form/Select';
import { InputJson } from 'src/mockingbird/components/form/InputJson';
import type { TGRPCMock } from 'src/mockingbird/models/mock/types';
import JSONRequest from './JSONRequest';
import { mapGrpcToFormData, mapFormDataToGrpc } from './utils';
import { SCOPES } from './refs';
import type { TGRPCFormData } from './types';

type Props = {
  labels: string[];
  serviceId: string;
  data?: TGRPCMock;
  actions?: ReactNode;
  submitText?: string;
  submitDisabled?: boolean;
  disabled?: boolean;
  onSubmit: (data: TGRPCFormData) => void;
};

export default function FormGrpc(props: Props) {
  const {
    labels,
    serviceId = '',
    data,
    actions,
    submitText = 'Создать',
    submitDisabled = false,
    disabled = false,
    onSubmit: onSubmitParent,
  } = props;
  const defaultValues = mapGrpcToFormData(serviceId, data);
  const { control, watch, handleSubmit } = useForm<TGRPCFormData>({
    defaultValues,
    mode: 'onBlur',
  });
  const scope = watch('scope');
  const onSubmit = useCallback(
    (formData: TGRPCFormData) => onSubmitParent(formData),
    [onSubmitParent]
  );
  const onGetValues = useCallback(() => {
    return mapFormDataToGrpc(watch(), serviceId);
  }, [watch, serviceId]);
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input
        name="name"
        label="Название"
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputSearchTagged
        name="labels"
        label="Лейблы"
        options={labels}
        control={control as any}
        disabled={disabled}
        mb="sm"
      />
      <Flex mb="sm">
        <Box sx={{ width: '50%' }}>
          <Select
            name="scope"
            label="Время жизни"
            options={SCOPES}
            control={control as any}
            disabled={disabled}
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
              disabled={disabled}
            />
          </Box>
        ) : (
          <Box sx={{ width: '50%' }} />
        )}
      </Flex>
      <Input
        name="methodName"
        label="Метод"
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <Title order={4}>Запрос</Title>
      {!disabled && (
        <AttachFile
          name="requestCodecs"
          label="Proto файл"
          control={control as any}
          single
          required
          mb="sm"
        />
      )}
      {disabled && (
        <Textarea
          value={watch('requestSchema')}
          label="Proto схема"
          minRows={15}
        />
      )}
      <Input
        name="requestClass"
        label="Класс"
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputJson
        name="requestPredicates"
        label="Предикаты"
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <Title order={4}>Ответ</Title>
      {!disabled && (
        <AttachFile
          name="responseCodecs"
          label="Proto файл"
          control={control as any}
          single
          required
          mb="sm"
        />
      )}
      {disabled && (
        <Textarea
          value={watch('responseSchema')}
          label="Proto схема"
          minRows={15}
        />
      )}
      <Input
        name="responseClass"
        label="Класс"
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputJson
        name="response"
        label="Ответ"
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputJson
        name="state"
        label="Предикат для поиска состояния"
        control={control as any}
        disabled={disabled}
        mb="sm"
      />
      <InputJson
        name="seed"
        label="Генерация переменных"
        control={control as any}
        disabled={disabled}
        mb="sm"
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
        <Button type="submit" size="md" disabled={disabled || submitDisabled}>
          {submitText}
        </Button>
      </Group>
    </form>
  );
}
