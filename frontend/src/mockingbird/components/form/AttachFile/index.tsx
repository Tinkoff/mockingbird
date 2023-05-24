import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import type { FileInputProps } from '@mantine/core';
import { FileInput } from '@mantine/core';
import { extractError } from 'src/mockingbird/infrastructure/helpers/forms';

type Props = FileInputProps & {
  name: string;
  control: Control;
  required?: boolean;
  single?: boolean;
};

export default function AttachFile(props: Props) {
  const {
    name,
    control,
    required = false,
    single = false,
    ...restProps
  } = props;
  const { field, formState, fieldState } = useController({
    name,
    control,
    rules: {
      required,
    },
  });
  const { onChange: onChangeField } = field;
  const onChange = useCallback(
    (value: File | File[]) => {
      if (Array.isArray(value)) {
        onChangeField(value);
      } else {
        onChangeField([value]);
      }
    },
    [onChangeField]
  );
  const defaultPlaceholder = single ? 'Выберите файл' : 'Выберите файлы';
  const uiPlaceholder = restProps.placeholder || defaultPlaceholder;
  const uiError =
    extractError(name, formState.errors) || fieldState.error?.message;
  const uiValue = single ? field.value && field.value[0] : field.value;
  return (
    <FileInput
      {...restProps}
      {...field}
      value={uiValue}
      error={uiError}
      onChange={onChange}
      placeholder={uiPlaceholder}
      required={required}
      multiple={!single}
      clearable
    />
  );
}
