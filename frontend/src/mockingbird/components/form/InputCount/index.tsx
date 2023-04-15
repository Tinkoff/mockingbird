import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import type { NumberInputProps } from '@mantine/core';
import { NumberInput } from '@mantine/core';
import { extractError } from 'src/mockingbird/infrastructure/helpers/forms';

type Props = Omit<NumberInputProps, 'name'> & {
  name: string;
  control: Control;
};

export default function InputCount(props: Props) {
  const {
    name,
    label,
    control,
    required = false,
    disabled = false,
    min,
    max,
    ...restProps
  } = props;
  const { field, formState, fieldState } = useController({
    name,
    control,
    rules: {
      required,
    },
    defaultValue: min,
  });
  const { onChange } = field;
  const handleChange = useCallback((value) => onChange(value), [onChange]);
  const uiError =
    extractError(name, formState.errors) || fieldState.error?.message;
  return (
    <NumberInput
      {...restProps}
      {...field}
      required={required}
      onChange={handleChange}
      label={label}
      error={uiError}
      disabled={disabled}
      min={min}
      max={max}
    />
  );
}
