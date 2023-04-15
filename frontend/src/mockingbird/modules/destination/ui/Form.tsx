import React, { useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { Button } from '@mantine/core';
import { Input } from 'src/mockingbird/components/form/Input';
import { InputJson } from 'src/mockingbird/components/form/InputJson';
import { validateJSONArray } from 'src/mockingbird/infrastructure/helpers/forms';
import { mapDestinationToFormData } from '../utils';
import type { Destination, DestinationFormData } from '../types';

type Props = {
  data?: Destination;
  submitText?: string;
  disabled?: boolean;
  onSubmit: (data: DestinationFormData) => void;
};

export default function Form(props: Props) {
  const {
    data,
    submitText = 'Создать',
    disabled = false,
    onSubmit: onSubmitParent,
  } = props;
  const defaultValues = mapDestinationToFormData(data);
  const { control, handleSubmit } = useForm<DestinationFormData>({
    defaultValues,
    mode: 'onBlur',
  });
  const onSubmit = useCallback(
    (formData: DestinationFormData) => onSubmitParent(formData),
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
        validate={validateJSONArray}
        disabled={disabled}
        mb="sm"
      />
      <InputJson
        name="shutdown"
        label="shutdown"
        control={control as any}
        validate={validateJSONArray}
        disabled={disabled}
        mb="sm"
      />
      <Button type="submit" size="md" disabled={disabled}>
        {submitText}
      </Button>
    </form>
  );
}
