import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import PlatformTextarea from '@platform-ui/textarea';

interface Props {
  name: string;
  label: string;
  control: Control;
  required?: boolean;
  disabled?: boolean;
  validate?: (value: string) => string | undefined;
}

export default function Textarea(props: Props) {
  const {
    name,
    label,
    control,
    required = false,
    disabled = false,
    validate,
  } = props;
  const { field } = useController({
    name,
    control,
    rules: {
      required,
      validate,
    },
    defaultValue: '',
  });
  const { onChange } = field;
  const handleChange = useCallback(
    (_, { value }) => onChange(value),
    [onChange]
  );
  return (
    <PlatformTextarea
      {...field}
      onChange={handleChange}
      label={label}
      required={required}
      disabled={disabled}
    />
  );
}
