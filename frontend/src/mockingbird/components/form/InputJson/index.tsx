import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import { JsonInput } from '@mantine/core';
import type { JsonInputProps } from '@mantine/core';
import {
  validateJSON,
  extractError,
} from 'src/mockingbird/infrastructure/helpers/forms';

type Props = Omit<JsonInputProps, 'name'> & {
  name: string;
  control: Control;
  validate?: (value: string) => string | undefined;
};

export function InputJson(props: Props) {
  const {
    name,
    label,
    control,
    required = false,
    disabled = false,
    validate = validateJSON,
    error = '',
    ...restProps
  } = props;
  const { field, fieldState, formState } = useController({
    name,
    control,
    rules: {
      required,
      validate,
    },
    defaultValue: '',
  });
  const { onChange } = field;
  const handleChange = useCallback((value) => onChange(value), [onChange]);
  const formError = extractError(name, formState.errors);
  const uiError = error || formError || fieldState.error?.message;
  return (
    <JsonInput
      {...restProps}
      {...field}
      error={error || formError || fieldState.error?.message}
      validationError={uiError}
      onChange={handleChange}
      label={label}
      required={required}
      disabled={disabled}
      minRows={12}
    />
  );
}
