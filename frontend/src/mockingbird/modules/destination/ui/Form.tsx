import React, { useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { FormRow } from '@platform-ui/form';
import {
  extractError,
  validateJSON,
  validateJSONArray,
} from 'src/mockingbird/infrastructure/helpers/forms';
import Input from 'src/mockingbird/components/form/Input';
import Textarea from 'src/mockingbird/components/form/Textarea';
import ButtonSubmit from 'src/mockingbird/components/ButtonSubmit';
import { mapDestinationToFormData } from '../utils';
import type { Destination, DestinationFormData } from '../types';

interface Props {
  data?: Destination;
  submitText?: string;
  disabled?: boolean;
  onSubmit: (data: DestinationFormData) => void;
}

export default function Form(props: Props) {
  const {
    data,
    submitText = 'Создать',
    disabled = false,
    onSubmit: onSubmitParent,
  } = props;
  const defaultValues = mapDestinationToFormData(data);
  const {
    control,
    formState: { errors },
    handleSubmit,
  } = useForm<DestinationFormData>({
    defaultValues,
  });
  const onSubmit = useCallback(
    (formData: DestinationFormData) => onSubmitParent(formData),
    [onSubmitParent]
  );
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <FormRow {...extractError('name', errors)}>
        <Input
          name="name"
          label="Название"
          control={control}
          disabled={disabled}
          readOnly={Boolean(data)}
          required
        />
      </FormRow>
      <FormRow {...extractError('description', errors)}>
        <Input
          name="description"
          label="Описание"
          control={control}
          disabled={disabled}
          required
        />
      </FormRow>
      <FormRow {...extractError('request', errors)}>
        <Textarea
          name="request"
          label="Запрос"
          validate={validateJSON}
          control={control}
          disabled={disabled}
          required
        />
      </FormRow>
      <FormRow {...extractError('init', errors)}>
        <Textarea
          name="init"
          label="init"
          validate={validateJSONArray}
          control={control}
          disabled={disabled}
        />
      </FormRow>
      <FormRow {...extractError('shutdown', errors)}>
        <Textarea
          name="shutdown"
          label="shutdown"
          validate={validateJSONArray}
          control={control}
          disabled={disabled}
        />
      </FormRow>
      <ButtonSubmit disabled={disabled}>{submitText}</ButtonSubmit>
    </form>
  );
}
