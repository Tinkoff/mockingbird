import React from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import { Select as SelectMantine } from '@mantine/core';
import type { SelectProps } from '@mantine/core';
import { extractError } from 'src/mockingbird/infrastructure/helpers/forms';

export type Props = Omit<SelectProps, 'data'> & {
  options: SelectProps['data'];
  name: string;
  control: Control;
};

export default function Select(props: Props) {
  const { name, control, required = false, options, defaultValue = '' } = props;
  const { field, formState, fieldState } = useController({
    name,
    control,
    rules: {
      required,
    },
    defaultValue,
  });
  const uiError =
    extractError(name, formState.errors) || fieldState.error?.message;
  return (
    <SelectMantine
      {...props}
      {...field}
      data={options}
      required={required}
      error={uiError}
    />
  );
}
