import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import PlatformInputCount from '@platform-ui/inputCount';

interface Props {
  name: string;
  label: string;
  control: Control;
  required?: boolean;
  disabled?: boolean;
  min?: number;
  max?: number;
}

export default function InputCount(props: Props) {
  const {
    name,
    label,
    control,
    required = false,
    disabled = false,
    min,
    max,
  } = props;
  const { field } = useController({
    name,
    control,
    rules: {
      required,
    },
    defaultValue: min,
  });
  const { onChange } = field;
  const handleChange = useCallback(
    (_, { value }) => onChange(value),
    [onChange]
  );
  return (
    <PlatformInputCount
      {...field}
      onChange={handleChange}
      label={label}
      required={required}
      disabled={disabled}
      min={min}
      max={max}
    />
  );
}
