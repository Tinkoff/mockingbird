import React, { useCallback, useEffect, useState } from 'react';
import type { Control, WatchInternal } from 'react-hook-form';
import { useForm } from 'react-hook-form';
import { Modal as Popup, Button, Group } from '@mantine/core';
import { InputJson } from 'src/mockingbird/components/form/InputJson';
import Destinations from 'src/mockingbird/modules/destinations';
import { Input } from 'src/mockingbird/components/form/Input';
import Select from 'src/mockingbird/components/form/Select';
import {
  CALLBACK_TYPES,
  CALLBACK_HTTP_RESPONSE_TYPES,
  DEFAULT_CALLBACK_HTTP,
  DEFAULT_CALLBACK_MESSAGE,
} from './refs';
import type { TFormCallback } from './types';

type Props = {
  serviceId: string;
  onSave: (callback: TFormCallback) => void;
  onClose: () => void;
  // eslint-disable-next-line react/no-unused-prop-types
  callback?: TFormCallback;
};

export default function CallbackPopup({
  callback,
  serviceId,
  onSave,
  onClose,
}: Props) {
  const [type, setType] = useState(
    callback ? callback.type : CALLBACK_TYPES[0].value
  );
  const Form = type === 'http' ? HTTPForm : MessageForm;
  const onChangeType = useCallback((t) => setType(t), []);
  const edit = Boolean(callback);
  const title = edit ? 'Редактирование коллбэка' : 'Создание коллбэка';
  return (
    <Popup size="lg" opened onClose={onClose} title={title}>
      <Form
        edit={edit}
        callback={callback}
        serviceId={serviceId}
        onChangeType={onChangeType}
        onSave={onSave}
        onClose={onClose}
      />
    </Popup>
  );
}

type FormProps = Props & {
  edit: boolean;
  onChangeType: (type: string) => void;
};

function HTTPForm({ callback, ...props }: FormProps) {
  const defaultValues = callback || DEFAULT_CALLBACK_HTTP;
  return (
    <FormBase
      {...props}
      defaultType="http"
      defaultValues={defaultValues}
      Fields={MTTPFormFields}
    />
  );
}

function MTTPFormFields({ control, watch }: FieldsProps) {
  const responseMode = watch('responseMode');
  return (
    <>
      <InputJson name="request" label="Запрос" control={control} required />
      <Select
        name="responseMode"
        label="Тип ответа"
        options={CALLBACK_HTTP_RESPONSE_TYPES}
        control={control}
      />
      {responseMode && (
        <InputJson
          name="persist"
          label="Данные, записываемые в базу"
          control={control}
          required
        />
      )}
    </>
  );
}

function MessageForm({ callback, ...props }: FormProps) {
  const defaultValues = callback || DEFAULT_CALLBACK_MESSAGE;
  return (
    <FormBase
      {...props}
      defaultType="message"
      defaultValues={defaultValues}
      Fields={MessageFormFields}
    />
  );
}

function MessageFormFields({ control, serviceId }: FieldsProps) {
  return (
    <>
      <Destinations
        name="destination"
        label="Получатель событий"
        serviceId={serviceId}
        control={control}
        required
      />
      <InputJson name="output" label="Ответ" control={control} required />
    </>
  );
}

type FieldsProps = {
  // eslint-disable-next-line react/no-unused-prop-types
  watch: WatchInternal<any>;
  control: Control;
  // eslint-disable-next-line react/no-unused-prop-types
  serviceId: string;
};

type FormBaseProps = FormProps & {
  edit: boolean;
  Fields: React.FC<FieldsProps>;
  defaultType: string;
  defaultValues: Partial<TFormCallback>;
};

function FormBase(props: FormBaseProps) {
  const {
    edit,
    Fields,
    defaultType,
    defaultValues,
    serviceId,
    onChangeType,
    onSave,
    onClose,
  } = props;
  const { control, watch, handleSubmit } = useForm<TFormCallback>({
    defaultValues,
    mode: 'onBlur',
  });
  const type = watch('type');
  useEffect(() => {
    if (type !== defaultType) onChangeType(type);
  }, [type, defaultType, onChangeType]);
  const onSubmit = useCallback(
    (e) => {
      e.stopPropagation();
      return handleSubmit(onSave)(e);
    },
    [handleSubmit, onSave]
  );
  return (
    <form onSubmit={onSubmit}>
      <Select
        name="type"
        label="Тип"
        options={CALLBACK_TYPES}
        control={control as any}
        disabled={edit}
        required
      />
      <Fields control={control as any} watch={watch} serviceId={serviceId} />
      <Input
        name="delay"
        label="Задержка"
        description="Паттерн: d+ (d|day|h|hour|m|min|minute|s|sec|second|ms|milli|millisecond|µs|micro|microsecond|ns|nano|nanosecond)"
        control={control as any}
        mb="lg"
      />
      <Group>
        <Button size="md" type="submit">
          {edit ? 'Сохранить' : 'Создать'}
        </Button>
        <Button size="md" variant="outline" onClick={onClose}>
          Отмена
        </Button>
      </Group>
    </form>
  );
}
