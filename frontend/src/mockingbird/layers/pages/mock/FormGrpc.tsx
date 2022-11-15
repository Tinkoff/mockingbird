import React, { useCallback } from 'react';
import { useForm } from 'react-hook-form';
import Accordion from '@platform-ui/accordion';
import { FormFieldset, FormGroup, FormRow } from '@platform-ui/form';
import PlatformTextarea from '@platform-ui/textarea';
import {
  extractError,
  validateJSON,
} from 'src/mockingbird/infrastructure/helpers/forms';
import AttachFile from 'src/mockingbird/components/form/AttachFile';
import Input from 'src/mockingbird/components/form/Input';
import InputCount from 'src/mockingbird/components/form/InputCount';
import InputSearchTagged from 'src/mockingbird/components/form/InputSearchTagged';
import Select from 'src/mockingbird/components/form/Select';
import Textarea from 'src/mockingbird/components/form/Textarea';
import ButtonSubmit from 'src/mockingbird/components/ButtonSubmit';
import type { THTTPMock } from 'src/mockingbird/models/mock/types';
import JSONRequest from './JSONRequest';
import { mapGrpcToFormData, mapFormDataToGrpc } from './utils';
import { SCOPES } from './refs';
import type { TGRPCFormData } from './types';

interface Props {
  labels: string[];
  serviceId: string;
  data?: THTTPMock;
  actions?: React.ReactElement;
  submitText?: string;
  submitDisabled?: boolean;
  disabled?: boolean;
  onSubmit: (data: TGRPCFormData) => void;
}

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
  const {
    control,
    watch,
    formState: { errors },
    handleSubmit,
  } = useForm<TGRPCFormData>({
    defaultValues,
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
      <FormRow {...extractError('name', errors)}>
        <Input
          name="name"
          label="Название"
          control={control}
          disabled={disabled}
          required
        />
      </FormRow>
      <FormRow {...extractError('labels', errors)}>
        <InputSearchTagged
          name="labels"
          label="Лейблы"
          options={labels}
          control={control}
          disabled={disabled}
        />
      </FormRow>
      <FormGroup>
        <FormRow {...extractError('scope', errors)}>
          <Select
            name="scope"
            label="Время жизни"
            options={SCOPES}
            control={control}
            disabled={disabled}
            required
          />
        </FormRow>
        {scope === 'countdown' && (
          <FormRow {...extractError('times', errors)}>
            <InputCount
              name="times"
              label="Количество срабатываний"
              min={1}
              control={control}
              disabled={disabled}
            />
          </FormRow>
        )}
      </FormGroup>
      <FormRow {...extractError('methodName', errors)}>
        <Input
          name="methodName"
          label="Метод"
          control={control}
          disabled={disabled}
          required
        />
      </FormRow>
      <FormFieldset legend="Запрос">
        {!disabled && (
          <FormRow
            outerLabel="Proto файл"
            {...extractError('requestCodecs', errors)}
          >
            <AttachFile
              name="requestCodecs"
              control={control}
              single
              required
            />
          </FormRow>
        )}
        {disabled && (
          <FormRow>
            <PlatformTextarea
              value={watch('requestSchema')}
              label="Proto схема"
              rows={15}
            />
          </FormRow>
        )}
        <FormRow {...extractError('requestClass', errors)}>
          <Input
            name="requestClass"
            label="Класс"
            control={control}
            disabled={disabled}
            required
          />
        </FormRow>
        <FormRow {...extractError('requestPredicates', errors)}>
          <Textarea
            name="requestPredicates"
            label="Предикаты"
            validate={validateJSON}
            control={control}
            disabled={disabled}
            required
          />
        </FormRow>
      </FormFieldset>
      <FormFieldset legend="Ответ">
        {!disabled && (
          <FormRow
            outerLabel="Proto файл"
            {...extractError('responseCodecs', errors)}
          >
            <AttachFile
              name="responseCodecs"
              control={control}
              single
              required
            />
          </FormRow>
        )}
        {disabled && (
          <FormRow>
            <PlatformTextarea
              value={watch('responseSchema')}
              label="Proto схема"
              rows={15}
            />
          </FormRow>
        )}
        <FormRow {...extractError('responseClass', errors)}>
          <Input
            name="responseClass"
            label="Класс"
            control={control}
            disabled={disabled}
            required
          />
        </FormRow>
        <FormRow {...extractError('response', errors)}>
          <Textarea
            name="response"
            label="Ответ"
            validate={validateJSON}
            control={control}
            disabled={disabled}
            required
          />
        </FormRow>
      </FormFieldset>
      <FormRow {...extractError('state', errors)}>
        <Textarea
          name="state"
          label="Предикат для поиска состояния"
          validate={validateJSON}
          control={control}
          disabled={disabled}
        />
      </FormRow>
      <FormRow {...extractError('seed', errors)}>
        <Textarea
          name="seed"
          label="Генерация переменных"
          validate={validateJSON}
          control={control}
          disabled={disabled}
        />
      </FormRow>
      <FormRow>
        <JSONRequest getValues={onGetValues} />
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
      <ButtonSubmit disabled={disabled || submitDisabled}>
        {submitText}
      </ButtonSubmit>
    </form>
  );
}
