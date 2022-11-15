import React, { useCallback, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from '@tramvai/module-router';
import { useActions, useStoreSelector } from '@tramvai/state';
import { FormFieldset, FormRow } from '@platform-ui/form';
import Popup from '@platform-ui/popup';
import { addToastAction } from 'src/infrastructure/notifications';
import { extractError } from 'src/mockingbird/infrastructure/helpers/forms';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import Input from 'src/mockingbird/components/form/Input';
import ButtonSubmit from 'src/mockingbird/components/ButtonSubmit';
import { getPathMocks } from 'src/mockingbird/paths';
import { createAction, resetCreateStateAction } from '../actions/createAction';
import createServiceStore from '../reducers/createStore';
import type { Service } from '../types';

interface Props {
  opened: boolean;
  onClose: () => void;
}

export default function CreatePopup({ opened, onClose }: Props) {
  const create = useActions(createAction);
  const { status, id, errorMessage } = useStoreSelector(
    createServiceStore,
    selectorAsIs
  );
  const resetCreateState = useActions(resetCreateStateAction);
  useEffect(() => resetCreateState, [resetCreateState]);
  const addToast = useActions(addToastAction);
  const navigate = useNavigate();
  useEffect(() => {
    if (status === 'complete' && id) {
      addToast({
        type: 'success',
        title: 'Сервис успешно создан',
        timer: 3000,
      });
      navigate(getPathMocks(id));
    } else if (status === 'error' && errorMessage) {
      addToast({
        type: 'error',
        title: 'Произошла ошибка при создании',
        children: errorMessage,
        timer: 5000,
      });
    }
  }, [status, id, errorMessage, addToast, navigate]);
  const {
    control,
    formState: { errors },
    handleSubmit,
  } = useForm<Service>({
    defaultValues: {
      name: '',
      suffix: '',
    },
  });
  const onSubmit = useCallback((data: Service) => create(data), [create]);
  return (
    <Popup opened={opened} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <FormFieldset legend="Создание сервиса">
          <FormRow {...extractError('name', errors)}>
            <Input name="name" label="Название" control={control} required />
          </FormRow>
          <FormRow {...extractError('suffix', errors)}>
            <Input name="suffix" label="Суффикс" control={control} required />
          </FormRow>
        </FormFieldset>
        <ButtonSubmit disabled={status === 'loading'}>Создать</ButtonSubmit>
      </form>
    </Popup>
  );
}
