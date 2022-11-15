import React, { useCallback } from 'react';
import { useForm } from 'react-hook-form';
import Accordion from '@platform-ui/accordion';
import { FormRow } from '@platform-ui/form';
import {
  extractError,
  validateJSON,
  validateJSONArray,
} from 'src/mockingbird/infrastructure/helpers/forms';
import Input from 'src/mockingbird/components/form/Input';
import Textarea from 'src/mockingbird/components/form/Textarea';
import ButtonSubmit from 'src/mockingbird/components/ButtonSubmit';
import { mapSourceToFormData } from '../utils';
import type { Source, SourceFormData } from '../types';

interface Props {
  actions?: React.ReactElement;
  data?: Source;
  submitText?: string;
  disabled?: boolean;
  onSubmit: (data: SourceFormData) => void;
}

export default function Form(props: Props) {
  const {
    actions,
    data,
    submitText = 'Создать',
    disabled = false,
    onSubmit: onSubmitParent,
  } = props;
  const defaultValues = mapSourceToFormData(data);
  const {
    control,
    formState: { errors },
    handleSubmit,
  } = useForm<SourceFormData>({
    defaultValues,
  });
  const onSubmit = useCallback(
    (formData: SourceFormData) => onSubmitParent(formData),
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
      {actions && (
        <FormRow>
          <Accordion
            flatCorners="true"
            data={[
              {
                title: 'Действия',
                content: actions,
              },
            ]}
          />
        </FormRow>
      )}
      <ButtonSubmit disabled={disabled}>{submitText}</ButtonSubmit>
    </form>
  );
}
