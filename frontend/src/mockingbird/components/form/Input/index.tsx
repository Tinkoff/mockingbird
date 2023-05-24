import React from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import { TextInput } from '@mantine/core';
import type { TextInputProps } from '@mantine/core';
import { extractError } from 'src/mockingbird/infrastructure/helpers/forms';

type Props = TextInputProps & {
  name: string;
  control: Control;
};

export function Input(props: Props) {
  const {
    name,
    label,
    control,
    required = false,
    disabled = false,
    ...restProps
  } = props;
  const ctrl = useController({
    name,
    control,
    rules: {
      required,
    },
    defaultValue: '',
  });
  const uiError =
    extractError(name, ctrl.formState.errors) || ctrl.fieldState.error?.message;
  return (
    <TextInput
      {...restProps}
      {...ctrl.field}
      error={uiError}
      required={required}
      label={label}
      disabled={disabled}
    />
  );
}
