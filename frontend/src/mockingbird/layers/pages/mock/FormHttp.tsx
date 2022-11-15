import React, { useCallback, useState } from 'react';
import { useForm } from 'react-hook-form';
import Accordion from '@platform-ui/accordion';
import { FormGroup, FormRow } from '@platform-ui/form';
import {
  extractError,
  validateJSON,
} from 'src/mockingbird/infrastructure/helpers/forms';
import Input from 'src/mockingbird/components/form/Input';
import InputCount from 'src/mockingbird/components/form/InputCount';
import InputSearchTagged from 'src/mockingbird/components/form/InputSearchTagged';
import Select from 'src/mockingbird/components/form/Select';
import Textarea from 'src/mockingbird/components/form/Textarea';
import ToggleBlock from 'src/mockingbird/components/form/ToggleBlock';
import ButtonSubmit from 'src/mockingbird/components/ButtonSubmit';
import type { THTTPMock } from 'src/mockingbird/models/mock/types';
import Callbacks from './Callbacks';
import JSONRequest from './JSONRequest';
import { mapStubToFormData, mapFormDataToStub } from './utils';
import { SCOPES, METHODS } from './refs';
import type { THTTPFormData, TFormCallback } from './types';

interface Props {
  labels: string[];
  serviceId: string;
  data?: THTTPMock;
  actions?: React.ReactElement;
  submitText?: string;
  submitDisabled?: boolean;
  onSubmit: (data: THTTPFormData, callbacks: TFormCallback[]) => void;
}

export default function FormHttp(props: Props) {
  const {
    labels,
    serviceId = '',
    data,
    actions,
    submitText = 'Создать',
    submitDisabled = false,
    onSubmit: onSubmitParent,
  } = props;
  const { callbacks: defaultCallbacks, ...defaultValues } = mapStubToFormData(
    serviceId,
    data
  );
  const [callbacks, setCallbacks] = useState<TFormCallback[]>(defaultCallbacks);
  const {
    control,
    watch,
    formState: { errors },
    handleSubmit,
  } = useForm<THTTPFormData>({
    defaultValues,
  });
  const scope = watch('scope');
  const onSubmit = useCallback(
    (formData: THTTPFormData) => onSubmitParent(formData, callbacks),
    [callbacks, onSubmitParent]
  );
  const onGetValues = useCallback(() => {
    return mapFormDataToStub(watch(), callbacks, serviceId);
  }, [watch, callbacks, serviceId]);
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <FormRow {...extractError('name', errors)}>
        <Input name="name" label="Название" control={control} required />
      </FormRow>
      <FormRow {...extractError('labels', errors)}>
        <InputSearchTagged
          name="labels"
          label="Лейблы"
          options={labels}
          control={control}
        />
      </FormRow>
      <FormGroup>
        <FormRow {...extractError('scope', errors)}>
          <Select
            name="scope"
            label="Время жизни"
            options={SCOPES}
            control={control}
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
            />
          </FormRow>
        )}
      </FormGroup>
      <FormRow {...extractError('method', errors)}>
        <Select
          name="method"
          label="Метод"
          options={METHODS}
          control={control}
          required
        />
      </FormRow>
      <FormGroup>
        <FormRow
          {...extractError('path', errors)}
          message={`Без префикса /${serviceId}. Пример: /demo`}
        >
          <Input name="path" label="Путь" control={control} required />
        </FormRow>
        <FormRow>
          <ToggleBlock
            name="isPathPattern"
            label="Путь-регулярка"
            control={control}
          />
        </FormRow>
      </FormGroup>
      <FormRow {...extractError('request', errors)}>
        <Textarea
          name="request"
          label="Запрос"
          validate={validateJSON}
          control={control}
          required
        />
      </FormRow>
      <FormRow {...extractError('response', errors)}>
        <Textarea
          name="response"
          label="Ответ"
          validate={validateJSON}
          control={control}
          required
        />
      </FormRow>
      <FormRow {...extractError('state', errors)}>
        <Textarea
          name="state"
          label="Предикат для поиска состояния"
          validate={validateJSON}
          control={control}
        />
      </FormRow>
      <FormRow {...extractError('persist', errors)}>
        <Textarea
          name="persist"
          label="Данные, записываемые в базу"
          validate={validateJSON}
          control={control}
        />
      </FormRow>
      <FormRow {...extractError('seed', errors)}>
        <Textarea
          name="seed"
          label="Генерация переменных"
          validate={validateJSON}
          control={control}
        />
      </FormRow>
      <FormRow>
        <Callbacks
          serviceId={serviceId}
          callbacks={callbacks}
          onChange={setCallbacks}
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
      <ButtonSubmit disabled={submitDisabled}>{submitText}</ButtonSubmit>
    </form>
  );
}
