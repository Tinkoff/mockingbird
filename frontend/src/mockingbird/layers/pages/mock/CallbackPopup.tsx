import React, { useCallback, useEffect, useState } from 'react';
import type { FieldErrors, Control, WatchInternal } from 'react-hook-form';
import { useForm } from 'react-hook-form';
import Button from '@platform-ui/button';
import { FormFieldset, FormRow } from '@platform-ui/form';
import Popup from '@platform-ui/popup';
import {
  extractError,
  validateJSON,
} from 'src/mockingbird/infrastructure/helpers/forms';
import Destinations from 'src/mockingbird/modules/destinations';
import Input from 'src/mockingbird/components/form/Input';
import Select from 'src/mockingbird/components/form/Select';
import Textarea from 'src/mockingbird/components/form/Textarea';
import {
  CALLBACK_TYPES,
  CALLBACK_HTTP_RESPONSE_TYPES,
  DEFAULT_CALLBACK_HTTP,
  DEFAULT_CALLBACK_MESSAGE,
} from './refs';
import type { TFormCallback } from './types';
import styles from './CallbackPopup.css';

interface Props {
  serviceId: string;
  callback?: TFormCallback;
  onSave: (callback: TFormCallback) => void;
  onClose: () => void;
}

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
  return (
    <Popup opened onClose={onClose}>
      <Form
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
  onChangeType: (type: string) => void;
};

function HTTPForm({ callback, ...props }: FormProps) {
  const defaultValues = callback || DEFAULT_CALLBACK_HTTP;
  return (
    <FormBase
      {...props}
      edit={Boolean(callback)}
      defaultType="http"
      defaultValues={defaultValues}
      Fields={MTTPFormFields}
    />
  );
}

function MTTPFormFields({ control, watch, errors }: FieldsProps) {
  const responseMode = watch('responseMode');
  return (
    <>
      <FormRow {...extractError('request', errors)}>
        <Textarea
          name="request"
          label="Запрос"
          validate={validateJSON}
          control={control}
          required
        />
      </FormRow>
      <FormRow {...extractError('responseMode', errors)}>
        <Select
          name="responseMode"
          label="Тип ответа"
          options={CALLBACK_HTTP_RESPONSE_TYPES}
          control={control}
        />
      </FormRow>
      {responseMode && (
        <FormRow {...extractError('persist', errors)}>
          <Textarea
            name="persist"
            label="Данные, записываемые в базу"
            validate={validateJSON}
            control={control}
            required
          />
        </FormRow>
      )}
    </>
  );
}

function MessageForm({ callback, ...props }: FormProps) {
  const defaultValues = callback || DEFAULT_CALLBACK_MESSAGE;
  return (
    <FormBase
      {...props}
      edit={Boolean(callback)}
      defaultType="message"
      defaultValues={defaultValues}
      Fields={MessageFormFields}
    />
  );
}

function MessageFormFields({ control, errors, serviceId }: FieldsProps) {
  return (
    <>
      <FormRow {...extractError('destination', errors)}>
        <Destinations
          name="destination"
          label="Получатель событий"
          serviceId={serviceId}
          control={control}
          required
        />
      </FormRow>
      <FormRow {...extractError('output', errors)}>
        <Textarea
          name="output"
          label="Ответ"
          validate={validateJSON}
          control={control}
          required
        />
      </FormRow>
    </>
  );
}

type FieldsProps = {
  errors: FieldErrors;
  watch: WatchInternal<any>;
  control: Control;
  serviceId: string;
};

type FormBaseProps = FormProps & {
  edit: boolean;
  Fields: React.FC<FieldsProps>;
  defaultType: string;
  defaultValues: TFormCallback;
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
  const {
    control,
    watch,
    formState: { errors },
    handleSubmit,
  } = useForm<TFormCallback>({
    defaultValues,
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
      <FormFieldset
        legend={edit ? 'Редактирование коллбэка' : 'Создание коллбэка'}
      >
        <FormRow {...extractError('type', errors)}>
          <Select
            name="type"
            label="Тип"
            options={CALLBACK_TYPES}
            control={control}
            disabled={edit}
            required
          />
        </FormRow>
        <Fields
          control={control}
          watch={watch}
          errors={errors}
          serviceId={serviceId}
        />
        <FormRow
          {...extractError('delay', errors)}
          message="Паттерн: d+ (d|day|h|hour|m|min|minute|s|sec|second|ms|milli|millisecond|µs|micro|microsecond|ns|nano|nanosecond)"
        >
          <Input name="delay" label="Задержка" control={control} />
        </FormRow>
      </FormFieldset>
      <div className={styles.buttons}>
        <Button size="l" type="submit">
          {edit ? 'Сохранить' : 'Создать'}
        </Button>
        <Button size="l" theme="secondary" onClick={onClose}>
          Отмена
        </Button>
      </div>
    </form>
  );
}
