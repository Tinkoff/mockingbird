import React, { useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { Button, Accordion } from '@mantine/core';
import { validateJSONArray } from 'src/mockingbird/infrastructure/helpers/forms';
import { Input } from 'src/mockingbird/components/form/Input';
import { InputJson } from 'src/mockingbird/components/form/InputJson';
import { mapSourceToFormData } from '../utils';
import type { Source, SourceFormData } from '../types';

type Props = {
  actions?: React.ReactElement;
  data?: Source;
  submitText?: string;
  disabled?: boolean;
  onSubmit: (data: SourceFormData) => void;
};

export default function Form(props: Props) {
  const {
    actions,
    data,
    submitText = 'Создать',
    disabled = false,
    onSubmit: onSubmitParent,
  } = props;
  const defaultValues = mapSourceToFormData(data);
  const { control, handleSubmit } = useForm<SourceFormData>({
    defaultValues,
    mode: 'onBlur',
  });
  const onSubmit = useCallback(
    (formData: SourceFormData) => onSubmitParent(formData),
    [onSubmitParent]
  );
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input
        name="name"
        label="Название"
        control={control as any}
        disabled={disabled || Boolean(data)}
        required
        mb="sm"
      />
      <Input
        name="description"
        label="Описание"
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputJson
        name="request"
        label="Запрос"
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputJson
        name="init"
        label="init"
        control={control as any}
        disabled={disabled}
        validate={validateJSONArray}
        mb="sm"
      />
      <InputJson
        name="shutdown"
        label="shutdown"
        control={control as any}
        disabled={disabled}
        validate={validateJSONArray}
        mb="md"
      />
      {actions && (
        <Accordion variant="contained" mb="md">
          <Accordion.Item value="actions">
            <Accordion.Control>Действия</Accordion.Control>
            <Accordion.Panel>{actions}</Accordion.Panel>
          </Accordion.Item>
        </Accordion>
      )}
      <Button type="submit" size="md" disabled={disabled}>
        {submitText}
      </Button>
    </form>
  );
}
